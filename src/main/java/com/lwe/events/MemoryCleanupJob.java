package com.lwe.events;

import com.lwe.core.repository.EntityMemoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Scheduled-Job: Lässt alte NPC-Erinnerungen verblassen.
 * - Sentiment driftet täglich um 1 Richtung 0
 * - Erinnerungen mit sentiment = 0 älter als 30 Tage werden gelöscht
 *
 * Läuft täglich um 03:30 UTC.
 */
@Service
public class MemoryCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(MemoryCleanupJob.class);

    private final EntityMemoryRepository repo;

    public MemoryCleanupJob(EntityMemoryRepository repo) {
        this.repo = repo;
    }

    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void decayMemories() {
        var cutoff = Instant.now().minusSeconds(86400 * 30); // 30 Tage

        // Lösche alte Erinnerungen mit Sentiment=0
        var oldMemories = repo.findByCreatedAtBeforeAndSentiment(cutoff, 0);
        repo.deleteAll(oldMemories);

        // Sentiment-Drift für Erinnerungen älter 7 Tage
        var driftCutoff = Instant.now().minusSeconds(86400 * 7);
        var driftMemories = repo.findByCreatedAtBefore(driftCutoff);
        for (var mem : driftMemories) {
            var s = mem.getSentiment();
            if (s > 0) {
                mem.setSentiment(Math.max(0, s - 1));
            } else if (s < 0) {
                mem.setSentiment(Math.min(0, s + 1));
            }
        }
        repo.saveAll(driftMemories);

        log.info("Memory cleanup: {} removed, {} decayed", oldMemories.size(), driftMemories.size());
    }
}
