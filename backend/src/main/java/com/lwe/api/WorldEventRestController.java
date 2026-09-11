package com.lwe.api;

import com.lwe.api.dto.WorldEventListResponse;
import com.lwe.api.dto.WorldEventResponse;
import com.lwe.core.domain.User;
import com.lwe.core.repository.WorldEventRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/worlds/{worldId}/events")
public class WorldEventRestController {

    private final WorldEventRepository eventRepo;
    private final com.lwe.core.util.WorldAccess worldAccess;

    public WorldEventRestController(WorldEventRepository eventRepo, com.lwe.core.util.WorldAccess worldAccess) {
        this.eventRepo = eventRepo;
        this.worldAccess = worldAccess;
    }

    @GetMapping
    public ResponseEntity<WorldEventListResponse> listEvents(@PathVariable UUID worldId,
                                                              @RequestParam(defaultValue = "0") long since,
                                                              @RequestParam(defaultValue = "50") int limit,
                                                              @AuthenticationPrincipal User user) {
        // P2-Audit: Events nur fuer Welt-Mitglieder; BOT/ADMIN fuer Polling.
        if (!"BOT".equals(user.getRole()) && !"ADMIN".equals(user.getRole())) {
            worldAccess.requireAccess(worldId, user.getId());
        }
        var raw = eventRepo.findByWorldIdAndIdGreaterThanOrderByIdAsc(worldId, since);
        var events = raw.size() > limit ? raw.subList(0, limit) : raw;
        return ResponseEntity.ok(new WorldEventListResponse(events.stream()
            .map(e -> new WorldEventResponse(
                e.getId(), e.getEventType(),
                e.getCampaignId() != null ? e.getCampaignId().toString() : "",
                e.getSourceEntityId() != null ? e.getSourceEntityId().toString() : "",
                e.getTargetEntityId() != null ? e.getTargetEntityId().toString() : "",
                e.getPayloadJson(), e.getCreatedAt().toString()))
            .toList()));
    }
}
