package com.lwe.core.service;

import com.lwe.core.domain.EntityEvent;
import com.lwe.core.repository.EntityEventRepository;
import com.lwe.core.repository.GameEntityRepository;
import com.lwe.core.repository.LocationRepository;
import com.lwe.core.repository.QuestRepository;
import com.lwe.core.repository.RegionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service für Entity-spezifische Ereignis-Logs.
 *
 * <p>Ermöglicht das Erzeugen und Abfragen von Events für Regionen, Orte und NPCs.
 * Jedes Event wird in {@code entity_events} persistiert und kann später vom KI-Bot
 * als Kontext für NPC-Entscheidungen genutzt werden.
 *
 * @see <a href="../../../../docs/WORLD-DEPTH.md">docs/WORLD-DEPTH.md</a>
 */
@Service
public class EntityEventService {

    private final EntityEventRepository repo;
    private final GameEntityRepository entityRepo;
    private final RegionRepository regionRepo;
    private final LocationRepository locationRepo;
    private final QuestRepository questRepo;

    public EntityEventService(EntityEventRepository repo, GameEntityRepository entityRepo,
                              RegionRepository regionRepo, LocationRepository locationRepo,
                              QuestRepository questRepo) {
        this.repo = repo;
        this.entityRepo = entityRepo;
        this.regionRepo = regionRepo;
        this.locationRepo = locationRepo;
        this.questRepo = questRepo;
    }

    /** N3-Audit: Welt zur Entity-Referenz aufloesen (fuer Access-Checks). */
    public java.util.Optional<UUID> resolveWorldId(String entityType, UUID entityId) {
        if (entityType == null || entityId == null) return java.util.Optional.empty();
        return switch (entityType.toLowerCase()) {
            case "pc", "npc", "entity", "faction" -> entityRepo.findById(entityId)
                .map(com.lwe.core.domain.GameEntity::getWorldId);
            case "region" -> regionRepo.findById(entityId)
                .map(com.lwe.core.domain.Region::getWorldId);
            case "location" -> locationRepo.findById(entityId)
                .flatMap(l -> regionRepo.findById(l.getRegionId()))
                .map(com.lwe.core.domain.Region::getWorldId);
            case "quest" -> questRepo.findById(entityId)
                .map(com.lwe.core.domain.Quest::getWorldId);
            default -> java.util.Optional.empty();
        };
    }

    @Transactional
    public EntityEvent publish(String entityType, UUID entityId, String eventType,
                                String title, String description, int importance,
                                UUID sourceEntityId) {
        var event = new EntityEvent(entityType, entityId, eventType, title, description, importance);
        if (sourceEntityId != null) {
            event.setSourceEntityId(sourceEntityId);
        }
        return repo.save(event);
    }

    /**
     * Liefert die letzten {@code limit} Events für eine Entity.
     */
    public List<EntityEvent> getEvents(String entityType, UUID entityId, int limit) {
        var events = repo.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId);
        return events.size() > limit ? events.subList(0, limit) : events;
    }

    public EntityEvent getById(Long id) {
        return repo.findById(id)
            .orElseThrow(() -> new EntityEventException("EVENT_NOT_FOUND", "Event not found: " + id));
    }

    public static class EntityEventException extends RuntimeException {
        private final String errorCode;
        public EntityEventException(String errorCode, String message) { super(message); this.errorCode = errorCode; }
        public String getErrorCode() { return errorCode; }
    }
}
