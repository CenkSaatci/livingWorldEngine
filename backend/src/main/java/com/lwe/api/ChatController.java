package com.lwe.api;

import com.lwe.api.dto.ApiResponse;
import com.lwe.core.domain.ChatMessage;
import com.lwe.core.domain.User;
import com.lwe.core.repository.ChatMessageRepository;
import com.lwe.core.repository.WorldRepository;
import com.lwe.core.util.WorldAccess;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class ChatController {

    private final SimpMessagingTemplate messaging;
    private final ChatMessageRepository chatRepo;
    private final WorldRepository worldRepo;
    private final WorldAccess worldAccess;

    public ChatController(SimpMessagingTemplate messaging, ChatMessageRepository chatRepo,
                          WorldRepository worldRepo, WorldAccess worldAccess) {
        this.messaging = messaging;
        this.chatRepo = chatRepo;
        this.worldRepo = worldRepo;
        this.worldAccess = worldAccess;
    }

    @MessageMapping("/chat/{worldId}")
    public void handleChat(Map<String, Object> payload) {
        var worldId = payload.get("worldId");
        var message = Map.of(
            "sender", payload.getOrDefault("sender", "Player"),
            "text", payload.getOrDefault("text", ""),
            "timestamp", Instant.now().toString());
        persist(worldId, message);
        messaging.convertAndSend("/topic/world/" + worldId,
            Map.of("event_type", "CHAT_MESSAGE", "payload", message));
    }

    @PostMapping("/api/v1/chat/{worldId}")
    public ResponseEntity<ApiResponse> postChat(@PathVariable String worldId,
                                                 @RequestBody Map<String, Object> body,
                                                 @AuthenticationPrincipal User user) {
        var wid = UUID.fromString(worldId);
        requireMember(wid, user.getId());
        var message = Map.of(
            "sender", body.getOrDefault("sender", "Player"),
            "text", body.getOrDefault("text", ""),
            "timestamp", Instant.now().toString());
        persist(wid, message);
        messaging.convertAndSend("/topic/world/" + worldId,
            Map.of("event_type", "CHAT_MESSAGE", "payload", message));
        return ResponseEntity.ok(new ApiResponse("sent"));
    }

    /** B5: Verlauf (neueste 50, chronologisch). */
    @GetMapping("/api/v1/chat/{worldId}")
    public ResponseEntity<List<Map<String, Object>>> history(@PathVariable String worldId,
                                                             @AuthenticationPrincipal User user) {
        var wid = UUID.fromString(worldId);
        requireMember(wid, user.getId());
        var messages = chatRepo.findTop50ByWorldIdOrderByCreatedAtDesc(wid).stream()
            .sorted(Comparator.comparing(ChatMessage::getCreatedAt))
            .map(m -> {
                Map<String, Object> row = Map.of(
                    "sender", m.getSender(),
                    "text", m.getText(),
                    "timestamp", m.getCreatedAt().toString());
                return row;
            })
            .toList();
        return ResponseEntity.ok(messages);
    }

    private void requireMember(UUID worldId, UUID userId) {
        worldRepo.findById(worldId)
            .orElseThrow(() -> new WorldAccess.WorldAccessException("WORLD_NOT_FOUND", "World not found"));
        worldAccess.requireRead(worldId, userId);
    }

    private void persist(Object worldId, Map<String, Object> message) {
        try {
            var text = String.valueOf(message.getOrDefault("text", ""));
            if (text.isBlank()) return;
            var wid = worldId instanceof UUID u ? u : UUID.fromString(String.valueOf(worldId));
            if (worldRepo.findById(wid).isEmpty()) return;
            chatRepo.save(new ChatMessage(wid,
                String.valueOf(message.getOrDefault("sender", "Player")), text));
        } catch (Exception ignored) {
            // Historie darf den Live-Chat nie brechen.
        }
    }
}
