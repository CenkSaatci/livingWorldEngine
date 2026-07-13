package com.lwe.api;

import com.lwe.core.domain.EntityEvent;
import com.lwe.core.service.EntityEventService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/entity-events")
public class EntityEventController {

    private final EntityEventService service;

    public EntityEventController(EntityEventService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<?> publish(@Valid @RequestBody PublishRequest req) {
        var event = service.publish(req.entityType(), req.entityId(), req.eventType(),
            req.title(), req.description(), req.importance(), req.sourceEntityId());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(event));
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam String entityType,
                                  @RequestParam UUID entityId,
                                  @RequestParam(defaultValue = "10") @Max(50) int limit) {
        var events = service.getEvents(entityType, entityId, limit)
            .stream().map(this::toResponse).toList();
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ResponseEntity.ok(toResponse(service.getById(id)));
    }

    private Map<String, Object> toResponse(EntityEvent e) {
        return Map.of(
            "id", e.getId(),
            "entity_type", e.getEntityType(),
            "entity_id", e.getEntityId(),
            "event_type", e.getEventType(),
            "title", e.getTitle(),
            "description", e.getDescription() != null ? e.getDescription() : "",
            "importance", e.getImportance(),
            "created_at", e.getCreatedAt().toString()
        );
    }

    public record PublishRequest(
        @NotBlank String entityType,
        @NotNull UUID entityId,
        @NotBlank String eventType,
        @NotBlank String title,
        String description,
        @Min(1) @Max(5) int importance,
        UUID sourceEntityId
    ) {}
}
