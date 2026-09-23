package com.lwe.core.service;

import com.lwe.core.domain.ChatMessage;
import com.lwe.core.domain.GameEntity;
import com.lwe.core.repository.ChatMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ADR-015: Chat-Auslieferung für POI-Aktionen.
 * <ul>
 *   <li>{@code public} — persistiert und an {@code /topic/world/{id}} (wie der normale Chat).</li>
 *   <li>{@code actor} — transient an den Ausführenden ({@code /user/queue/poi}), nicht im Verlauf.</li>
 * </ul>
 * Beide Wege sind best-effort: Chat darf die Aktion nie brechen.
 */
@Component
public class PoiChat {

    private static final Logger log = LoggerFactory.getLogger(PoiChat.class);

    private final ChatMessageRepository chatRepo;
    private final SimpMessagingTemplate messaging;

    public PoiChat(ChatMessageRepository chatRepo, SimpMessagingTemplate messaging) {
        this.chatRepo = chatRepo;
        this.messaging = messaging;
    }

    public void deliver(String mode, UUID worldId, GameEntity actor, String actionName,
                        String text, List<Map<String, Object>> applied) {
        if ("none".equals(mode) || text == null || text.isBlank()) return;
        if ("public".equals(mode)) {
            var message = Map.<String, Object>of(
                "sender", actor.getName(),
                "text", text,
                "timestamp", Instant.now().toString());
            try {
                chatRepo.save(new ChatMessage(worldId, actor.getName(), text));
            } catch (Exception ignored) {
                // Historie darf die Aktion nie brechen.
            }
            try {
                messaging.convertAndSend("/topic/world/" + worldId,
                    Map.of("event_type", "CHAT_MESSAGE", "payload", message));
            } catch (Exception e) {
                log.warn("POI-Chat-Broadcast fehlgeschlagen: {}", e.getMessage());
            }
        } else {
            try {
                var recipient = actor.getOwnerUserId() != null
                    ? actor.getOwnerUserId().toString() : actor.getId().toString();
                messaging.convertAndSendToUser(recipient, "/queue/poi",
                    Map.of("action", actionName, "text", text, "effects", applied));
            } catch (Exception e) {
                log.warn("POI-Direktnachricht fehlgeschlagen: {}", e.getMessage());
            }
        }
    }
}
