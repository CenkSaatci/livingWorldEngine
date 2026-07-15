package com.lwe.api;

import com.lwe.core.domain.Ability;
import com.lwe.core.domain.Ability.AbilityType;
import com.lwe.core.domain.User;
import com.lwe.core.service.AbilityService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class AbilityController {

    private final AbilityService service;

    public AbilityController(AbilityService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/worlds/{worldId}/abilities")
    public ResponseEntity<AbilityResponse> create(@PathVariable UUID worldId,
                                                    @Valid @RequestBody CreateRequest req,
                                                    @AuthenticationPrincipal User user) {
        var ability = service.create(worldId, user.getId(), req.name(), req.type(),
            req.description(), req.effectsJson(), req.statBonusesJson(),
            req.apCost(), req.cooldownRounds(), req.targetType());
        return ResponseEntity.status(HttpStatus.CREATED).body(AbilityResponse.from(ability));
    }

    @GetMapping("/api/v1/worlds/{worldId}/abilities")
    public ResponseEntity<List<AbilityResponse>> listByWorld(@PathVariable UUID worldId,
                                                              @AuthenticationPrincipal User user) {
        var abilities = service.listByWorld(worldId, user.getId())
            .stream().map(AbilityResponse::from).toList();
        return ResponseEntity.ok(abilities);
    }

    @GetMapping("/api/v1/abilities/{id}")
    public ResponseEntity<AbilityResponse> getById(@PathVariable UUID id,
                                                    @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(AbilityResponse.from(service.getById(id, user.getId())));
    }

    @PutMapping("/api/v1/abilities/{id}")
    public ResponseEntity<AbilityResponse> update(@PathVariable UUID id,
                                                    @Valid @RequestBody UpdateRequest req,
                                                    @AuthenticationPrincipal User user) {
        var ability = service.update(id, user.getId(), req.name(), req.description(),
            req.effectsJson(), req.statBonusesJson(), req.apCost(), req.cooldownRounds(), req.targetType());
        return ResponseEntity.ok(AbilityResponse.from(ability));
    }

    @DeleteMapping("/api/v1/abilities/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id,
                                        @AuthenticationPrincipal User user) {
        service.delete(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    public record CreateRequest(
        @NotBlank String name,
        AbilityType type,
        String description,
        String effectsJson,
        String statBonusesJson,
        int apCost,
        int cooldownRounds,
        String targetType
    ) {}

    public record UpdateRequest(
        String name,
        String description,
        String effectsJson,
        String statBonusesJson,
        Integer apCost,
        Integer cooldownRounds,
        String targetType
    ) {}

    public record AbilityResponse(
        UUID id, UUID worldId, String name, String type,
        String description, String effectsJson, String statBonusesJson,
        int apCost, int cooldownRounds, String targetType, String createdAt
    ) {
        static AbilityResponse from(Ability a) {
            return new AbilityResponse(
                a.getId(), a.getWorldId(), a.getName(), a.getType().name(),
                a.getDescription(), a.getEffectsJson(), a.getStatBonusesJson(),
                a.getApCost(), a.getCooldownRounds(), a.getTargetType(),
                a.getCreatedAt().toString());
        }
    }
}
