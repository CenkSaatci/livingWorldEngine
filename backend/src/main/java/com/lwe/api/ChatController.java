package com.lwe.api;

import com.lwe.api.dto.ApiResponse;
import com.lwe.core.domain.User;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
public class ChatController {

    private final SimpMessagingTemplate messaging;

    public ChatController(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    @MessageMapping("/chat/{worldId}")
    public void handleChat(Map<String, Object> payload) {
        var message = Map.of(
            "sender", payload.getOrDefault("sender", "Player"),
            "text", payload.getOrDefault("text", ""),
            "timestamp", Instant.now().toString());
        messaging.convertAndSend("/topic/world/" + payload.get("worldId"),
            Map.of("event_type", "CHAT_MESSAGE", "payload", message));
    }

    @PostMapping("/api/v1/chat/{worldId}")
    public ResponseEntity<ApiResponse> postChat(@PathVariable String worldId,
                                                 @RequestBody Map<String, Object> body,
                                                 @AuthenticationPrincipal User user) {
        var message = Map.of(
            "sender", body.getOrDefault("sender", "Player"),
            "text", body.getOrDefault("text", ""),
            "timestamp", Instant.now().toString());
        messaging.convertAndSend("/topic/world/" + worldId,
            Map.of("event_type", "CHAT_MESSAGE", "payload", message));
        return ResponseEntity.ok(new ApiResponse("sent"));
    }
}
