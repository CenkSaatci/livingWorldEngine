package com.lwe.api;

import com.lwe.core.domain.CombatSession;
import com.lwe.core.domain.User;
import com.lwe.core.service.CombatService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/combat")
public class CombatController {

    private final CombatService combatService;

    public CombatController(CombatService combatService) {
        this.combatService = combatService;
    }

    @PostMapping("/start")
    public ResponseEntity<?> start(@Valid @RequestBody StartRequest req,
                                   @AuthenticationPrincipal User user) {
        var session = combatService.startCombat(user.getId(), req.worldId(), req.participantIds());
        return ResponseEntity.status(HttpStatus.CREATED).body(sessionResponse(session));
    }

    @PostMapping("/{sessionId}/action")
    public ResponseEntity<?> action(@PathVariable UUID sessionId,
                                    @Valid @RequestBody ActionRequest req,
                                    @AuthenticationPrincipal User user) {
        var result = combatService.executeAction(user.getId(), sessionId,
            req.actorId(), req.actionType(), req.targetId(), req.itemId());
        return ResponseEntity.ok(Map.of(
            "action_type", result.actionType(),
            "total_damage", result.totalDamage(),
            "ap_remaining", result.apRemaining(),
            "success", result.success()
        ));
    }

    @PostMapping("/{sessionId}/next-turn")
    public ResponseEntity<?> nextTurn(@PathVariable UUID sessionId,
                                      @AuthenticationPrincipal User user) {
        var session = combatService.nextTurn(user.getId(), sessionId);
        return ResponseEntity.ok(sessionResponse(session));
    }

    @PostMapping("/{sessionId}/end")
    public ResponseEntity<?> end(@PathVariable UUID sessionId,
                                 @AuthenticationPrincipal User user) {
        var session = combatService.endCombat(user.getId(), sessionId);
        return ResponseEntity.ok(sessionResponse(session));
    }

    private Map<String, Object> sessionResponse(CombatSession s) {
        return Map.of(
            "id", s.getId(),
            "world_id", s.getWorldId(),
            "status", s.getStatus(),
            "round", s.getRound(),
            "current_turn_entity_id", s.getCurrentTurnEntityId() != null
                ? s.getCurrentTurnEntityId().toString() : "",
            "created_at", s.getCreatedAt().toString()
        );
    }

    public record StartRequest(
        @NotBlank UUID worldId,
        @NotEmpty List<UUID> participantIds
    ) {}

    public record ActionRequest(
        @NotBlank UUID actorId,
        @NotBlank String actionType,
        UUID targetId,
        UUID itemId
    ) {}
}