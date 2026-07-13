package com.lwe.api;

import com.lwe.core.domain.User;
import com.lwe.core.service.EntityService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/worlds/{worldId}/entities")
public class EntityController {

    private final EntityService entityService;

    public EntityController(EntityService entityService) {
        this.entityService = entityService;
    }

    @PostMapping
    public ResponseEntity<?> create(@PathVariable UUID worldId,
                                    @Valid @RequestBody CreateRequest req,
                                    @AuthenticationPrincipal User user) {
        var entity = entityService.create(worldId, user.getId(), req.entityType(),
            req.name(), req.attributesJson(), req.inventoryJson(),
            req.positionJson(), req.metadataJson(), req.factionId());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(entity));
    }

    @GetMapping
    public ResponseEntity<?> list(@PathVariable UUID worldId,
                                  @RequestParam(required = false) String type,
                                  @AuthenticationPrincipal User user) {
        var entities = entityService.list(worldId, user.getId(), type)
            .stream().map(this::toResponse).toList();
        return ResponseEntity.ok(entities);
    }

    @GetMapping("/{entityId}")
    public ResponseEntity<?> getById(@PathVariable UUID worldId,
                                     @PathVariable UUID entityId,
                                     @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(toResponse(entityService.getById(entityId, user.getId())));
    }

    @PatchMapping("/{entityId}")
    public ResponseEntity<?> update(@PathVariable UUID worldId,
                                    @PathVariable UUID entityId,
                                    @RequestBody UpdateRequest req,
                                    @AuthenticationPrincipal User user) {
        var entity = entityService.update(entityId, user.getId(),
            req.name(), req.attributesJson(), req.inventoryJson(),
            req.positionJson(), req.metadataJson());
        return ResponseEntity.ok(toResponse(entity));
    }

    @DeleteMapping("/{entityId}")
    public ResponseEntity<?> delete(@PathVariable UUID worldId,
                                    @PathVariable UUID entityId,
                                    @AuthenticationPrincipal User user) {
        entityService.delete(entityId, user.getId());
        return ResponseEntity.noContent().build();
    }

    private Map<String, Object> toResponse(com.lwe.core.domain.GameEntity e) {
        return Map.of(
            "id", e.getId(),
            "world_id", e.getWorldId(),
            "entity_type", e.getEntityType(),
            "name", e.getName(),
            "attributes_json", e.getAttributesJson(),
            "inventory_json", e.getInventoryJson(),
            "position_json", e.getPositionJson() != null ? e.getPositionJson() : "",
            "metadata_json", e.getMetadataJson(),
            "faction_id", e.getFactionId() != null ? e.getFactionId().toString() : "",
            "created_at", e.getCreatedAt().toString()
        );
    }

    public record CreateRequest(
        @NotBlank @Pattern(regexp = "^(PC|NPC|FACTION)$") String entityType,
        @NotBlank String name,
        String attributesJson,
        String inventoryJson,
        String positionJson,
        String metadataJson,
        UUID factionId
    ) {}

    public record UpdateRequest(
        String name,
        String attributesJson,
        String inventoryJson,
        String positionJson,
        String metadataJson
    ) {}
}