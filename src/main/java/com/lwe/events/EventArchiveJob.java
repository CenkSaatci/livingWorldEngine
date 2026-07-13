package com.lwe.events;

import com.lwe.core.repository.WorldEventRepository;
import jakarta.persistence.EntityManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Scheduled Job: Archiviert Events älter als 30 Tage von {@code world_events}
 * nach {@code world_events_archive}.
 *
 * <p>Läuft einmal täglich um 03:00 UTC.
 */
@Service
public class EventArchiveJob {

    private final EntityManager entityManager;
    private final WorldEventRepository eventRepo;

    public EventArchiveJob(EntityManager entityManager, WorldEventRepository eventRepo) {
        this.entityManager = entityManager;
        this.eventRepo = eventRepo;
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void archiveOldEvents() {
        var cutoff = Instant.now().minusSeconds(30L * 24 * 60 * 60);

        // 1. Kopieren nach archive
        var insertCount = entityManager.createNativeQuery(
            """
            INSERT INTO world_events_archive (world_id, event_type, source_entity_id,
                target_entity_id, payload_json, event_hash, created_at)
            SELECT world_id, event_type, source_entity_id, target_entity_id,
                payload_json, event_hash, created_at
            FROM world_events
            WHERE created_at < ?1
            """
        ).setParameter(1, cutoff)
         .executeUpdate();

        // 2. Löschen aus Haupttabelle
        entityManager.createNativeQuery(
            "DELETE FROM world_events WHERE created_at < ?1"
        ).setParameter(1, cutoff)
         .executeUpdate();

        if (insertCount > 0) {
            var logger = org.slf4j.LoggerFactory.getLogger(getClass());
            logger.info("Archived {} events older than {}", insertCount, cutoff);
        }
    }
}
