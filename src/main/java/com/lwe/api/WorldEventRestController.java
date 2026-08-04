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

    public WorldEventRestController(WorldEventRepository eventRepo) {
        this.eventRepo = eventRepo;
    }

    @GetMapping
    public ResponseEntity<WorldEventListResponse> listEvents(@PathVariable UUID worldId,
                                                              @RequestParam(defaultValue = "0") long since,
                                                              @RequestParam(defaultValue = "50") int limit,
                                                              @AuthenticationPrincipal User user) {
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
