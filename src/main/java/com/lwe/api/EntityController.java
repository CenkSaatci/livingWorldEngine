package com.lwe.api;

import com.lwe.api.dto.EntityResponse;
import com.lwe.api.dto.ApiResponse;
import com.lwe.api.dto.ErrorResponse;
import com.lwe.core.domain.User;
import com.lwe.core.service.EntityService;
import com.lwe.core.service.RestService;
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
    private final RestService restService;

    public EntityController(EntityService entityService, RestService restService) {
        this.entityService = entityService;
        this.restService = restService;
    }

    @PostMapping
    public ResponseEntity<EntityResponse> create(@PathVariable UUID worldId,
                                                 @Valid @RequestBody CreateRequest req,
                                                 @AuthenticationPrincipal User user) {
        var entity = entityService.create(worldId, user.getId(), req.entityType(),
            req.name(), req.attributesJson(), req.inventoryJson(),
            req.positionJson(), req.metadataJson(), req.factionId(),
            req.backstory(), req.age(), req.experienceLevel(), req.socialStanding());
        return ResponseEntity.status(HttpStatus.CREATED).body(EntityResponse.from(entity));
    }

    @GetMapping
    public ResponseEntity<?> list(@PathVariable UUID worldId,
                                  @RequestParam(required = false) String type,
                                  @AuthenticationPrincipal User user) {
        var entities = entityService.list(worldId, user.getId(), type)
            .stream().map(EntityResponse::from).toList();
        return ResponseEntity.ok(entities);
    }

    @GetMapping("/{entityId}")
    public ResponseEntity<EntityResponse> getById(@PathVariable UUID worldId,
                                                   @PathVariable UUID entityId,
                                                   @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(EntityResponse.from(entityService.getById(entityId, user.getId())));
    }

    @PatchMapping("/{entityId}")
    public ResponseEntity<EntityResponse> update(@PathVariable UUID worldId,
                                                  @PathVariable UUID entityId,
                                                  @RequestBody UpdateRequest req,
                                                  @AuthenticationPrincipal User user) {
        var entity = entityService.update(entityId, user.getId(),
            req.name(), req.attributesJson(), req.inventoryJson(),
            req.positionJson(), req.metadataJson(),
            req.backstory(), req.age(), req.experienceLevel(), req.socialStanding());
        return ResponseEntity.ok(EntityResponse.from(entity));
    }

    @PatchMapping("/{entityId}/attributes")
    public ResponseEntity<EntityResponse> updateAttributes(@PathVariable UUID worldId,
                                                          @PathVariable UUID entityId,
                                                          @RequestBody Map<String, Integer> attrs,
                                                          @AuthenticationPrincipal User user) {
        var entity = entityService.updateAttributes(entityId, user.getId(), attrs);
        return ResponseEntity.ok(EntityResponse.from(entity));
    }

    @PatchMapping("/{entityId}/progression")
    public ResponseEntity<EntityResponse> updateProgression(@PathVariable UUID worldId,
                                                            @PathVariable UUID entityId,
                                                            @RequestBody Map<String, Object> body,
                                                            @AuthenticationPrincipal User user) {
        var xp = ((Number) body.getOrDefault("experience_points", 0)).intValue();
        var entity = entityService.updateProgression(entityId, user.getId(), xp, null);
        return ResponseEntity.ok(EntityResponse.from(entity));
    }

    @PatchMapping("/{entityId}/skills")
    public ResponseEntity<EntityResponse> updateSkills(@PathVariable UUID worldId,
                                                       @PathVariable UUID entityId,
                                                       @RequestBody Map<String, Integer> skills,
                                                       @AuthenticationPrincipal User user) {
        var entity = entityService.updateSkills(entityId, user.getId(), skills);
        return ResponseEntity.ok(EntityResponse.from(entity));
    }

    @PatchMapping("/{entityId}/override")
    public ResponseEntity<EntityResponse> updateOverride(@PathVariable UUID worldId,
                                                          @PathVariable UUID entityId,
                                                          @RequestBody Map<String, Object> body,
                                                          @AuthenticationPrincipal User user) {
        var entity = entityService.updateOverrides(entityId, user.getId(), body);
        return ResponseEntity.ok(EntityResponse.from(entity));
    }

    @DeleteMapping("/{entityId}")
    public ResponseEntity<Void> delete(@PathVariable UUID worldId,
                                       @PathVariable UUID entityId,
                                       @AuthenticationPrincipal User user) {
        entityService.delete(entityId, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{entityId}/rest/short")
    public ResponseEntity<ApiResponse> shortRest(@PathVariable UUID entityId,
                                                  @AuthenticationPrincipal User user) {
        restService.shortRest(entityId, user.getId());
        return ResponseEntity.ok(new ApiResponse("Short rest completed"));
    }

    @PostMapping("/{entityId}/rest/long")
    public ResponseEntity<ApiResponse> longRest(@PathVariable UUID entityId,
                                                 @AuthenticationPrincipal User user) {
        restService.longRest(entityId, user.getId());
        return ResponseEntity.ok(new ApiResponse("Long rest completed"));
    }

    @PostMapping("/import")
    public ResponseEntity<EntityResponse> importEntity(@PathVariable UUID worldId,
                                                        @Valid @RequestBody CreateRequest req,
                                                        @AuthenticationPrincipal User user) {
        var entity = entityService.create(worldId, user.getId(), req.entityType(),
            req.name(), req.attributesJson(), req.inventoryJson(),
            req.positionJson(), req.metadataJson(), req.factionId(),
            req.backstory(), req.age(), req.experienceLevel(), req.socialStanding());
        return ResponseEntity.status(HttpStatus.CREATED).body(EntityResponse.from(entity));
    }

    private EntityResponse toResponse(com.lwe.core.domain.GameEntity e) {
        return EntityResponse.from(e);
    }

    public record CreateRequest(
        @NotBlank @Pattern(regexp = "^(PC|NPC|FACTION)$") String entityType,
        @NotBlank String name,
        String attributesJson,
        String inventoryJson,
        String positionJson,
        String metadataJson,
        UUID factionId,
        String backstory,
        Integer age,
        String experienceLevel,
        String socialStanding
    ) {}

    public record UpdateRequest(
        String name,
        String attributesJson,
        String inventoryJson,
        String positionJson,
        String metadataJson,
        String backstory,
        Integer age,
        String experienceLevel,
        String socialStanding
    ) {}
}