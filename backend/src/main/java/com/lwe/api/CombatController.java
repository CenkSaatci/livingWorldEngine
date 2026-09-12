package com.lwe.api;

import com.lwe.api.dto.CombatSessionResponse;
import com.lwe.api.dto.ParticipantResponse;
import com.lwe.core.domain.User;
import com.lwe.core.service.CombatService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    public ResponseEntity<CombatSessionResponse> start(@Valid @RequestBody StartRequest req,
                                                        @AuthenticationPrincipal User user) {
        var session = combatService.startCombat(user.getId(), req.worldId(), req.participantIds(), req.mapId(), req.campaignId());
        return ResponseEntity.status(HttpStatus.CREATED).body(CombatSessionResponse.from(session));
    }

    @PostMapping("/{sessionId}/action")
    public ResponseEntity<CombatSessionWithParticipants> action(@PathVariable UUID sessionId,
                                                                  @Valid @RequestBody ActionRequest req,
                                                                  @AuthenticationPrincipal User user) {
        var result = combatService.executeAction(user.getId(), sessionId,
            req.actorId(), req.actionType(), req.targetId(), req.itemId());
        var session = combatService.getSession(user.getId(), sessionId);
        var participants = combatService.getParticipants(sessionId);
        return ResponseEntity.ok(new CombatSessionWithParticipants(
            CombatSessionResponse.from(session), participants, ActionResultResponse.from(result)));
    }

    @PostMapping("/{sessionId}/maneuver")
    public ResponseEntity<CombatSessionWithParticipants> maneuver(@PathVariable UUID sessionId,
                                                                   @Valid @RequestBody ManeuverRequest req,
                                                                   @AuthenticationPrincipal User user) {
        combatService.executeManeuver(user.getId(), sessionId, req.actorId(), req.targetId(), req.maneuver());
        var session = combatService.getSession(user.getId(), sessionId);
        var participants = combatService.getParticipants(sessionId);
        return ResponseEntity.ok(new CombatSessionWithParticipants(
            CombatSessionResponse.from(session), participants));
    }

    @PostMapping("/{sessionId}/next-turn")
    public ResponseEntity<CombatSessionResponse> nextTurn(@PathVariable UUID sessionId,
                                                           @AuthenticationPrincipal User user) {
        var session = combatService.nextTurn(user.getId(), sessionId);
        return ResponseEntity.ok(CombatSessionResponse.from(session));
    }

    @GetMapping("/active")
    public ResponseEntity<CombatSessionWithParticipants> getActiveSession(
            @RequestParam UUID worldId,
            @AuthenticationPrincipal User user) {
        var opt = combatService.findActiveSession(user.getId(), worldId);
        if (opt.isEmpty()) return ResponseEntity.noContent().build();
        var session = opt.get();
        var participants = combatService.getParticipants(session.getId());
        return ResponseEntity.ok(new CombatSessionWithParticipants(
            CombatSessionResponse.from(session), participants));
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<CombatSessionWithParticipants> getSession(@PathVariable UUID sessionId,
                                                                     @AuthenticationPrincipal User user) {
        var session = combatService.getSession(user.getId(), sessionId);
        var participants = combatService.getParticipants(sessionId);
        return ResponseEntity.ok(new CombatSessionWithParticipants(
            CombatSessionResponse.from(session), participants));
    }

    @PostMapping("/{sessionId}/end")
    public ResponseEntity<CombatSessionResponse> end(@PathVariable UUID sessionId,
                                                      @AuthenticationPrincipal User user) {
        var session = combatService.endCombat(user.getId(), sessionId);
        return ResponseEntity.ok(CombatSessionResponse.from(session));
    }

    @PostMapping("/{sessionId}/ability")
    public ResponseEntity<CombatSessionWithParticipants> useAbility(@PathVariable UUID sessionId,
                                                                      @Valid @RequestBody AbilityRequest req,
                                                                      @AuthenticationPrincipal User user) {
        combatService.useAbility(user.getId(), sessionId,
            req.actorId(), req.abilityId(), req.targetId());
        var session = combatService.getSession(user.getId(), sessionId);
        var participants = combatService.getParticipants(sessionId);
        return ResponseEntity.ok(new CombatSessionWithParticipants(
            CombatSessionResponse.from(session), participants));
    }

    public record StartRequest(
        @NotNull UUID worldId,
        @NotEmpty List<UUID> participantIds,
        UUID mapId,
        UUID campaignId
    ) {}

    public record ActionRequest(
        @NotNull UUID actorId,
        @NotBlank String actionType,
        UUID targetId,
        UUID itemId
    ) {}

    public record ManeuverRequest(
        @NotNull UUID actorId,
        UUID targetId,
        @NotBlank String maneuver
    ) {}

    public record AbilityRequest(
        @NotNull UUID actorId,
        @NotNull UUID abilityId,
        UUID targetId
    ) {}

    public record CombatActionResultResponse(String actionType, int totalDamage,
                                              int apRemaining, boolean success) {}

    public record CombatSessionWithParticipants(CombatSessionResponse session,
                                                  List<ParticipantResponse> participants,
                                                  ActionResultResponse result) {
        public CombatSessionWithParticipants(CombatSessionResponse session,
                                             List<ParticipantResponse> participants) {
            this(session, participants, null);
        }
    }

    /** P1: additiv — UI kann Treffer/Fehlschlag unterscheiden (MISS-Feedback). */
    public record ActionResultResponse(String actionType, int totalDamage, int apCurrent) {
        static ActionResultResponse from(CombatService.CombatActionResult r) {
            return r == null ? null : new ActionResultResponse(r.actionType(), r.totalDamage(), r.apRemaining());
        }
    }
}