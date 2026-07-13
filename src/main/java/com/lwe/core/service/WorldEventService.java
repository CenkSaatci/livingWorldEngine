package com.lwe.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwe.core.domain.WorldEvent;
import com.lwe.core.repository.WorldEventRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

/**
 * Zentraler Event-Bus der LWE. Jede spiel-relevante Aktion erzeugt ein {@link WorldEvent},
 * das in der DB persistiert und per WebSocket an alle Subscriber des Topics
 * {@code /topic/world/{worldId}} broadcastet wird.
 *
 * <p>Idempotenz via {@code event_hash} (SHA-256 des serialisierten Payloads + eventType).
 * Retention: Events älter 30 Tage werden archiviert (siehe Phase 5).
 */
@Service
public class WorldEventService {

    private final WorldEventRepository eventRepo;
    private final SimpMessagingTemplate messaging;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WorldEventService(WorldEventRepository eventRepo, SimpMessagingTemplate messaging) {
        this.eventRepo = eventRepo;
        this.messaging = messaging;
    }

    @Transactional
    public long publish(UUID worldId, EventType eventType, UUID sourceEntityId,
                        UUID targetEntityId, Map<String, Object> payload) {
        var hash = computeHash(eventType.name(), payload);
        var event = new WorldEvent(worldId, eventType.name(), sourceEntityId, targetEntityId, payload, hash);
        event = eventRepo.save(event);

        // WebSocket-Broadcast an /topic/world/{worldId}
        if (messaging != null && worldId != null) {
            try {
                messaging.convertAndSend("/topic/world/" + worldId, Map.of(
                    "id", event.getId(),
                    "event_type", eventType.name(),
                    "source_entity_id", sourceEntityId != null ? sourceEntityId.toString() : "",
                    "target_entity_id", targetEntityId != null ? targetEntityId.toString() : "",
                    "payload", payload != null ? payload : Map.of(),
                    "created_at", event.getCreatedAt().toString()
                ));
            } catch (Exception ignored) {
                // WS nicht verfügbar (z. B. Tests) → ignorieren
            }
        }

        return event.getId();
    }

    private String computeHash(String eventType, Map<String, Object> payload) {
        try {
            var serialized = eventType + (payload != null ? objectMapper.writeValueAsString(payload) : "");
            var digest = MessageDigest.getInstance("SHA-256");
            var bytes = digest.digest(serialized.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }

    public enum EventType {
        PROBE_ROLLED,
        COMBAT_STARTED, COMBAT_ENDED, COMBAT_ACTION_EXECUTED,
        ENTITY_MOVED,
        ITEM_PICKED_UP, ITEM_EQUIPPED,
        FIRE_CREATED, FIRE_EXTINGUISHED,
        NPC_INTENT_PROPOSED, NPC_INTENT_APPROVED, NPC_INTENT_REJECTED,
        TURN_CHANGED, TIME_ADVANCED, TIME_PAUSED, TIME_RESUMED, TIME_MODE_CHANGED,
        ADVENTURE_STARTED, ADVENTURE_ADVANCED,
        WORLD_CREATED, SESSION_STARTED, SESSION_ENDED
    }
}