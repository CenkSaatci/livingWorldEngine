package com.lwe.api;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * OPTIONAL (P1-T07): Test-Endpunkt, der ein Event auf {@code /topic/world/{worldId}} published.
 * Ermöglicht WS-Verifikation ohne Spiellogik. Kann später entfernt werden.
 */
@RestController
@RequestMapping("/api/v1/test/ws")
public class TestWsController {

    private final SimpMessagingTemplate messaging;

    public TestWsController(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    @PostMapping("/{worldId}")
    public Map<String, Object> sendTestEvent(UUID worldId) {
        var event = Map.of(
            "event_type", "TEST_EVENT",
            "message", "Hello from LWE WebSocket!",
            "timestamp", Instant.now().toString()
        );
        messaging.convertAndSend("/topic/world/" + worldId, event);
        return Map.of("sent", true, "topic", "/topic/world/" + worldId);
    }
}