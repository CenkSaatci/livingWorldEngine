package com.lwe.api;

import com.lwe.api.dto.ApiResponse;
import com.lwe.core.domain.ChatMessage;
import com.lwe.core.domain.User;
import com.lwe.core.repository.ChatMessageRepository;
import com.lwe.core.repository.WorldRepository;
import com.lwe.core.util.WorldAccess;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
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

    /** C1-Audit: WS-Pfad jetzt mit Auth + Mitgliedschaftspruefung (wie StompController). */
    @MessageMapping("/chat/{worldId}")
    public void handleChat(@DestinationVariable String worldId,
                           @Payload Map<String, Object> payload,
                           Principal principal) {
        if (principal == null) {
            throw new WorldAccess.WorldAccessException("WS_AUTH_REQUIRED", "Authentication required");
        }
        var wid = parseWorldId(worldId);
        worldAccess.requireAccess(wid, UUID.fromString(principal.getName()));
        var message = sanitizeMessage(payload);
        if (message == null) return;
        persist(wid, message);
        broadcast(wid, message);
    }

    @PostMapping("/api/v1/chat/{worldId}")
    public ResponseEntity<ApiResponse> postChat(@PathVariable String worldId,
                                                 @RequestBody Map<String, Object> body,
                                                 @AuthenticationPrincipal User user) {
        var wid = parseWorldId(worldId);
        worldAccess.requireAccess(wid, user.getId()); // Audit: Schreiben = Mitglied
        var message = sanitizeMessage(body);
        if (message == null) return ResponseEntity.ok(new ApiResponse("ignored (empty)"));
        persist(wid, message);
        broadcast(wid, message);
        return ResponseEntity.ok(new ApiResponse("sent"));
    }

    /** B5: Verlauf (neueste 50, chronologisch). */
    @GetMapping("/api/v1/chat/{worldId}")
    public ResponseEntity<List<Map<String, Object>>> history(@PathVariable String worldId,
                                                             @AuthenticationPrincipal User user) {
        var wid = parseWorldId(worldId);
        requireWorld(wid);
        worldAccess.requireRead(wid, user.getId());
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

    private UUID parseWorldId(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            throw new WorldAccess.WorldAccessException("WS_PAYLOAD_INVALID", "Invalid world id");
        }
    }

    private void requireWorld(UUID worldId) {
        worldRepo.findById(worldId)
            .orElseThrow(() -> new WorldAccess.WorldAccessException("WORLD_NOT_FOUND", "World not found"));
    }

    /** Null-sichere Nachricht; leere Texte werden ignoriert (kein Broadcast). */
    private Map<String, Object> sanitizeMessage(Map<String, Object> payload) {
        if (payload == null) return null;
        var rawText = payload.get("text");
        var text = rawText == null ? "" : String.valueOf(rawText);
        if (text.isBlank()) return null;
        var rawSender = payload.get("sender");
        var sender = rawSender == null ? "Player" : String.valueOf(rawSender);
        return Map.of(
            "sender", sender,
            "text", text,
            "timestamp", Instant.now().toString());
    }

    private void broadcast(UUID worldId, Map<String, Object> message) {
        messaging.convertAndSend("/topic/world/" + worldId,
            Map.of("event_type", "CHAT_MESSAGE", "payload", message));
    }

    private void persist(UUID worldId, Map<String, Object> message) {
        try {
            if (worldRepo.findById(worldId).isEmpty()) return;
            chatRepo.save(new ChatMessage(worldId,
                String.valueOf(message.getOrDefault("sender", "Player")),
                String.valueOf(message.getOrDefault("text", ""))));
        } catch (Exception ignored) {
            // Historie darf den Live-Chat nie brechen.
        }
    }
}
