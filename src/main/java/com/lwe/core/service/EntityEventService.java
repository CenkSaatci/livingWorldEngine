package com.lwe.core.service;

import com.lwe.core.domain.EntityEvent;
import com.lwe.core.repository.EntityEventRepository;
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

    public EntityEventService(EntityEventRepository repo) {
        this.repo = repo;
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
