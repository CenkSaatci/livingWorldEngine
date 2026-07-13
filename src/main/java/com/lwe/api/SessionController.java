package com.lwe.api;

import com.lwe.core.domain.User;
import com.lwe.core.service.WorldEventService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Session-Lifecycle: Session starten/beenden für eine Welt.
 * Ermöglicht DM, eine Spielsession zu beginnen und zu beenden.
 */
@RestController
@RequestMapping("/api/v1/sessions")
public class SessionController {

    private final WorldEventService eventService;

    public SessionController(WorldEventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping("/{worldId}/start")
    public ResponseEntity<?> startSession(@PathVariable UUID worldId,
                                          @AuthenticationPrincipal User user) {
        eventService.publish(worldId, WorldEventService.EventType.SESSION_STARTED,
            null, null, Map.of("dm", user.getId().toString(), "startedAt", Instant.now().toString()));
        return ResponseEntity.ok(Map.of("session", "active", "world_id", worldId));
    }

    @PostMapping("/{worldId}/end")
    public ResponseEntity<?> endSession(@PathVariable UUID worldId,
                                        @AuthenticationPrincipal User user) {
        eventService.publish(worldId, WorldEventService.EventType.SESSION_ENDED,
            null, null, Map.of("dm", user.getId().toString(), "endedAt", Instant.now().toString()));
        return ResponseEntity.ok(Map.of("session", "ended", "world_id", worldId));
    }
}
