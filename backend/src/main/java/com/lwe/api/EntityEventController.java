package com.lwe.api;

import com.lwe.api.dto.EntityEventResponse;
import com.lwe.core.domain.User;
import com.lwe.core.service.EntityEventService;
import com.lwe.core.util.WorldAccess;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/entity-events")
public class EntityEventController {

    private final EntityEventService service;
    private final WorldAccess worldAccess;

    public EntityEventController(EntityEventService service, WorldAccess worldAccess) {
        this.service = service;
        this.worldAccess = worldAccess;
    }

    /** N3-Audit: nur BOT/ADMIN oder Mitglieder der zugehoerigen Welt. */
    private void requireAccess(String entityType, UUID entityId, User user) {
        if ("BOT".equals(user.getRole()) || "ADMIN".equals(user.getRole())) return;
        var worldId = service.resolveWorldId(entityType, entityId)
            .orElseThrow(() -> new ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN, "Unknown entity reference"));
        worldAccess.requireAccess(worldId, user.getId());
    }

    @PostMapping
    public ResponseEntity<EntityEventResponse> publish(@Valid @RequestBody PublishRequest req,
                                                        @AuthenticationPrincipal User user) {
        requireAccess(req.entityType(), req.entityId(), user);
        var event = service.publish(req.entityType(), req.entityId(), req.eventType(),
            req.title(), req.description(), req.importance(), req.sourceEntityId());
        return ResponseEntity.status(HttpStatus.CREATED).body(EntityEventResponse.from(event));
    }

    @GetMapping
    public ResponseEntity<List<EntityEventResponse>> list(@RequestParam String entityType,
                                                          @RequestParam UUID entityId,
                                                          @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit,
                                                          @AuthenticationPrincipal User user) {
        requireAccess(entityType, entityId, user);
        var events = service.getEvents(entityType, entityId, limit)
            .stream().map(EntityEventResponse::from).toList();
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntityEventResponse> getById(@PathVariable Long id,
                                                        @AuthenticationPrincipal User user) {
        var event = service.getById(id);
        requireAccess(event.getEntityType(), event.getEntityId(), user);
        return ResponseEntity.ok(EntityEventResponse.from(event));
    }

    public record PublishRequest(
        @NotBlank String entityType, @NotNull UUID entityId, @NotBlank String eventType,
        @NotBlank String title, String description, @Min(1) @Max(5) int importance,
        UUID sourceEntityId
    ) {}
}
