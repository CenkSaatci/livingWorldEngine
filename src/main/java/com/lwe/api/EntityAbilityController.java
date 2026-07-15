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
    public ResponseEntity<Void> assign(@PathVariable UUID entityId, @PathVariable UUID abilityId) {
        service.assign(entityId, abilityId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public ResponseEntity<List<AssignedAbilityResponse>> list(@PathVariable UUID entityId) {
        var assigned = service.listByEntity(entityId);
        var responses = assigned.stream()
            .map(ea -> {
                var ability = service.getAbility(ea.getAbilityId());
                return new AssignedAbilityResponse(
                    ea.getAbilityId(), ability.getName(), ability.getType().name(),
                    ability.getApCost(), ability.getDescription());
            })
            .filter(a -> "ACTIVE".equals(a.type))
            .toList();
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/{abilityId}")
    public ResponseEntity<Void> unassign(@PathVariable UUID entityId, @PathVariable UUID abilityId) {
        service.unassign(entityId, abilityId);
        return ResponseEntity.noContent().build();
    }

    public record AssignedAbilityResponse(UUID abilityId, String abilityName, String type,
                                           int apCost, String description) {}
}
