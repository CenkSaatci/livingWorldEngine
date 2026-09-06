package com.lwe.api;

import com.lwe.core.domain.Ability;
import com.lwe.core.domain.User;
import com.lwe.core.service.EntityAbilityService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/entities/{entityId}/abilities")
public class EntityAbilityController {

    private final EntityAbilityService service;

    public EntityAbilityController(EntityAbilityService service) {
        this.service = service;
    }

    @PostMapping("/{abilityId}")
    public ResponseEntity<Void> assign(@PathVariable UUID entityId, @PathVariable UUID abilityId,
                                       @AuthenticationPrincipal User user) {
        service.assign(entityId, abilityId, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public ResponseEntity<List<AssignedAbilityResponse>> list(@PathVariable UUID entityId,
                                                               @RequestParam(required = false) String type,
                                                               @AuthenticationPrincipal User user) {
        var assigned = service.listByEntity(entityId, user.getId());
        var responses = assigned.stream()
            .map(ea -> {
                var ability = service.getAbility(ea.getAbilityId());
                return new AssignedAbilityResponse(
                    ea.getAbilityId(), ability.getName(), ability.getType().name(),
                    ability.getApCost(), ability.getDescription());
            })
            .filter(a -> type == null || a.type().equalsIgnoreCase(type))
            .toList();
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/{abilityId}")
    public ResponseEntity<Void> unassign(@PathVariable UUID entityId, @PathVariable UUID abilityId,
                                         @AuthenticationPrincipal User user) {
        service.unassign(entityId, abilityId, user.getId());
        return ResponseEntity.noContent().build();
    }

    public record AssignedAbilityResponse(UUID abilityId, String abilityName, String type,
                                           int apCost, String description) {}
}
