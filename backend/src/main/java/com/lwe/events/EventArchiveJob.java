package com.lwe.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwe.core.repository.WorldEventRepository;
import com.lwe.core.repository.WorldRepository;
import jakarta.persistence.EntityManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Scheduled Job: Archiviert alte Events aus {@code world_events} nach
 * {@code world_events_archive}. Das Archivierungs-Intervall wird pro Welt
 * aus {@code worlds.settings_json.event_archive_days} gelesen (Default: 30 Tage).
 *
 * <p>Läuft einmal täglich um 03:00 UTC.
 * Kann manuell getriggert werden via {@code POST /api/v1/admin/events/archive}.
 */
@Service
public class EventArchiveJob {

    private final EntityManager entityManager;
    private final WorldEventRepository eventRepo;
    private final WorldRepository worldRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EventArchiveJob(EntityManager entityManager, WorldEventRepository eventRepo,
                           WorldRepository worldRepo) {
        this.entityManager = entityManager;
        this.eventRepo = eventRepo;
        this.worldRepo = worldRepo;
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void archiveOldEvents() {
        var worlds = worldRepo.findByActiveTrue();
        int totalArchived = 0;

        for (var world : worlds) {
            int archiveDays = resolveArchiveDays(world.getSettingsJson());
            var cutoff = Instant.now().minusSeconds(archiveDays * 24L * 60 * 60);
            totalArchived += archiveWorld(world.getId(), cutoff);
        }

        if (totalArchived > 0) {
            var logger = org.slf4j.LoggerFactory.getLogger(getClass());
            logger.info("Archived {} events across {} worlds", totalArchived, worlds.size());
        }
    }

    @Transactional
    public int archiveWorld(java.util.UUID worldId, Instant cutoff) {
        // 1. Kopieren nach archive
        var insertCount = entityManager.createNativeQuery(
            """
            INSERT INTO world_events_archive (world_id, event_type, source_entity_id,
                target_entity_id, payload_json, event_hash, created_at)
            SELECT world_id, event_type, source_entity_id, target_entity_id,
                payload_json, event_hash, created_at
            FROM world_events
            WHERE world_id = ?1 AND created_at < ?2
            """
        ).setParameter(1, worldId)
         .setParameter(2, cutoff)
         .executeUpdate();

        // 2. Löschen aus Haupttabelle
        entityManager.createNativeQuery(
            "DELETE FROM world_events WHERE world_id = ?1 AND created_at < ?2"
        ).setParameter(1, worldId)
         .setParameter(2, cutoff)
         .executeUpdate();

        return insertCount;
    }

    private int resolveArchiveDays(String settingsJson) {
        if (settingsJson == null || settingsJson.isBlank()) return 30;
        try {
            var tree = objectMapper.readTree(settingsJson);
            var val = tree.path("event_archive_days");
            return val.isInt() ? val.asInt() : 30;
        } catch (Exception e) {
            return 30;
        }
    }
}
