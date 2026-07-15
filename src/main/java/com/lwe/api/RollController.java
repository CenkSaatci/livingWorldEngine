package com.lwe.api;

import com.lwe.api.dto.ErrorResponse;
import com.lwe.api.dto.RollResponse;
import com.lwe.api.dto.SkillCheckResponse;
import com.lwe.core.domain.User;
import com.lwe.core.service.RollService;
import com.lwe.rules.DiceExpression;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rolls")
public class RollController {

    private final RollService rollService;

    public RollController(RollService rollService) {
        this.rollService = rollService;
    }

    @PostMapping("/free")
    public ResponseEntity<?> freeRoll(@Valid @RequestBody FreeRollRequest req) {
        try {
            var expr = new DiceExpression(req.expression());
            return ResponseEntity.ok(new RollResponse(
                req.expression(), expr.getRolls(), expr.getTotal(),
                expr.getModifier(), expr.getSides(), expr.getCount()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<SkillCheckResponse> roll(@Valid @RequestBody RollRequest req,
                                                    @AuthenticationPrincipal User user) {
        var result = rollService.executeRoll(
            user.getId(), req.worldId(), req.entityId(),
            req.skillId(), req.modifier(), req.target());

        return ResponseEntity.ok(new SkillCheckResponse(
            result.skillId(), result.expression(), result.dice(),
            result.total(), result.target(), result.success(), result.error()));
    }

    public record RollRequest(
        @NotBlank UUID worldId,
        @NotBlank UUID entityId,
        @NotBlank String skillId,
        int modifier,
        @PositiveOrZero int target
    ) {}

    public record FreeRollRequest(@NotBlank String expression) {}
}
