package com.lwe.api;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.util.Map;

/**
 * STOMP-Controller für Echtzeit-Kommunikation (Chat, Token-Bewegung).
 */
@Controller
public class StompController {

    private final SimpMessagingTemplate messaging;

    public StompController(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    @MessageMapping("/chat/{worldId}")
    public void handleChat(@DestinationVariable String worldId,
                           @Payload Map<String, String> payload) {
        var message = Map.of(
            "sender", payload.getOrDefault("sender", "unknown"),
            "text", payload.getOrDefault("text", ""),
            "timestamp", Instant.now().toString()
        );
        messaging.convertAndSend("/topic/world/" + worldId, Map.of(
            "event_type", "CHAT_MESSAGE",
            "payload", message
        ));
    }

    @MessageMapping("/token/move/{mapId}")
    public void handleTokenMove(@DestinationVariable String mapId,
                                @Payload Map<String, Object> payload) {
        messaging.convertAndSend("/topic/world/" + mapId, Map.of(
            "event_type", "TOKEN_MOVED",
            "payload", payload
        ));
    }
}
