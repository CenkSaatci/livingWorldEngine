package com.lwe.api;

import com.lwe.core.util.WorldAccess;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

/**
 * STOMP-Controller für Echtzeit-Kommunikation (Chat, Token-Bewegung).
 */
@Controller
public class StompController {

    private final SimpMessagingTemplate messaging;
    private final WorldAccess worldAccess;

    public StompController(SimpMessagingTemplate messaging, WorldAccess worldAccess) {
        this.messaging = messaging;
        this.worldAccess = worldAccess;
    }

    @MessageMapping("/token/move/{mapId}")
    public void handleTokenMove(@DestinationVariable String mapId,
                                @Payload Map<String, Object> payload,
                                Principal principal) {
        if (principal == null) {
            throw new WorldAccess.WorldAccessException("WS_AUTH_REQUIRED", "Authentication required");
        }
        UUID worldId;
        try {
            worldId = UUID.fromString(mapId);
        } catch (IllegalArgumentException e) {
            throw new WorldAccess.WorldAccessException("WS_PAYLOAD_INVALID", "Invalid map id");
        }
        if (payload == null || !payload.containsKey("entityId")) {
            throw new WorldAccess.WorldAccessException("WS_PAYLOAD_INVALID", "Malformed token move payload");
        }
        worldAccess.requireAccess(worldId, UUID.fromString(principal.getName()));

        messaging.convertAndSend("/topic/world/" + worldId, Map.of(
            "event_type", "TOKEN_MOVED",
            "payload", payload
        ));
    }
}
