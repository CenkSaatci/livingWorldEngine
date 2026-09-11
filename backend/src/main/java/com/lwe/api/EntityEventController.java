package com.lwe.api;

import com.lwe.api.dto.EntityEventResponse;
import com.lwe.core.service.EntityEventService;
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

    public EntityEventController(EntityEventService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<EntityEventResponse> publish(@Valid @RequestBody PublishRequest req) {
        var event = service.publish(req.entityType(), req.entityId(), req.eventType(),
            req.title(), req.description(), req.importance(), req.sourceEntityId());
        return ResponseEntity.status(HttpStatus.CREATED).body(EntityEventResponse.from(event));
    }

    @GetMapping
    public ResponseEntity<List<EntityEventResponse>> list(@RequestParam String entityType,
                                                          @RequestParam UUID entityId,
                                                          @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        var events = service.getEvents(entityType, entityId, limit)
            .stream().map(EntityEventResponse::from).toList();
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntityEventResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(EntityEventResponse.from(service.getById(id)));
    }

    public record PublishRequest(
        @NotBlank String entityType, @NotNull UUID entityId, @NotBlank String eventType,
        @NotBlank String title, String description, @Min(1) @Max(5) int importance,
        UUID sourceEntityId
    ) {}
}
