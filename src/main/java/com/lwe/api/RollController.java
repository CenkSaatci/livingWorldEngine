package com.lwe.api;

import com.lwe.core.domain.User;
import com.lwe.core.service.RollService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rolls")
public class RollController {

    private final RollService rollService;

    public RollController(RollService rollService) {
        this.rollService = rollService;
    }

    @PostMapping
    public ResponseEntity<?> roll(@Valid @RequestBody RollRequest req,
                                  @AuthenticationPrincipal User user) {
        var result = rollService.executeRoll(
            user.getId(), req.worldId(), req.entityId(),
            req.skillId(), req.modifier(), req.target()
        );

        return ResponseEntity.ok(Map.of(
            "skill_id", result.skillId(),
            "expression", result.expression(),
            "dice", result.dice(),
            "total", result.total(),
            "target", result.target(),
            "success", result.success(),
            "error", result.error()
        ));
    }

    public record RollRequest(
        @NotBlank UUID worldId,
        @NotBlank UUID entityId,
        @NotBlank String skillId,
        int modifier,
        @PositiveOrZero int target
    ) {}
}