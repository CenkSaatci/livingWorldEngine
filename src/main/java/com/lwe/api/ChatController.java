package com.lwe.api;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

/**
 * REST-Fallback für Chat – broadcastet Nachrichten an alle WS-Subscriber.
 * Langfristig durch STOMP `/app/chat/{worldId}` ersetzt (siehe StompController).
 */
@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final SimpMessagingTemplate messaging;

    public ChatController(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    @PostMapping("/{worldId}")
    public ResponseEntity<?> sendMessage(@PathVariable String worldId,
                                         @RequestBody Map<String, String> body) {
        var message = Map.of(
            "sender", body.getOrDefault("sender", "Player"),
            "text", body.getOrDefault("text", ""),
            "timestamp", Instant.now().toString()
        );
        messaging.convertAndSend("/topic/world/" + worldId, Map.of(
            "event_type", "CHAT_MESSAGE",
            "payload", message
        ));
        return ResponseEntity.ok(Map.of("sent", true));
    }
}
