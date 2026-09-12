package com.lwe.api;

import com.lwe.api.dto.ErrorResponse;
import com.lwe.api.dto.ProbeResponse;
import com.lwe.api.dto.RollResponse;
import com.lwe.api.dto.SkillCheckResponse;
import com.lwe.core.domain.User;
import com.lwe.core.service.ProbeService;
import com.lwe.core.service.RollService;
import com.lwe.rules.DiceExpression;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rolls")
public class RollController {

    private final RollService rollService;
    private final ProbeService probeService;

    public RollController(RollService rollService, ProbeService probeService) {
        this.rollService = rollService;
        this.probeService = probeService;
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

    @PostMapping("/probe")
    public ResponseEntity<ProbeResponse> probe(@Valid @RequestBody ProbeRequest req,
                                                @AuthenticationPrincipal User user) {
        var result = probeService.executeProbe(
            req.entityId(), user.getId(), req.skillName(),
            req.target(), req.advantage(), req.campaignId(),
            new ProbeService.ProbeOptions(
                req.difficulty() != null ? req.difficulty() : 0,
                req.difficultyKey(),
                req.bonusDice() != null ? req.bonusDice() : 0,
                req.penaltyDice() != null ? req.penaltyDice() : 0,
                req.useFate()),
            req.socialAction(), req.socialTargetId());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/cast")
    public ResponseEntity<ProbeService.CastResult> cast(@Valid @RequestBody CastRequest req,
                                                        @AuthenticationPrincipal User user) {
        var result = probeService.cast(
            req.entityId(), user.getId(), req.skillName(), req.campaignId(),
            req.target(), req.difficulty());
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<RollService.RollResult> roll(@Valid @RequestBody RollRequest req,
                                                       @AuthenticationPrincipal User user) {
        var result = rollService.executeRoll(
            user.getId(), req.worldId(), req.entityId(),
            req.skillId(), req.modifier(), req.target(), req.campaignId());
        return ResponseEntity.ok(result);
    }

    public record ProbeRequest(
        @NotNull UUID entityId,
        @NotBlank String skillName,
        int target,
        boolean advantage,
        UUID campaignId,
        Integer difficulty,
        String difficultyKey,
        Integer bonusDice,
        Integer penaltyDice,
        boolean useFate,
        String socialAction,
        UUID socialTargetId
    ) {}

    public record CastRequest(
        @NotNull UUID entityId,
        @NotBlank String skillName,
        UUID campaignId,
        Integer target,
        Integer difficulty
    ) {}

    public record RollRequest(
        @NotNull UUID worldId,
        @NotNull UUID entityId,
        @NotBlank String skillId,
        int modifier,
        @PositiveOrZero int target,
        UUID campaignId
    ) {}
    public record FreeRollRequest(@NotBlank String expression) {}
}
