package com.lwe.api;

import com.lwe.api.dto.GameSessionInfoResponse;
import com.lwe.core.domain.User;
import com.lwe.core.service.GameSessionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class GameSessionController {

    private final GameSessionService sessionService;

    public GameSessionController(GameSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping("/api/v1/sessions/start")
    public ResponseEntity<GameSessionInfoResponse> start(@Valid @RequestBody StartRequest req,
                                                          @AuthenticationPrincipal User user) {
        var session = sessionService.startSession(req.worldId(), req.campaignId(), user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(GameSessionInfoResponse.from(session));
    }

    @PostMapping("/api/v1/sessions/{id}/end")
    public ResponseEntity<GameSessionInfoResponse> end(@PathVariable UUID id,
                                                        @AuthenticationPrincipal User user) {
        var session = sessionService.endSession(id, user.getId());
        return ResponseEntity.ok(GameSessionInfoResponse.from(session));
    }

    @GetMapping("/api/v1/worlds/{worldId}/sessions")
    public ResponseEntity<List<GameSessionInfoResponse>> list(@PathVariable UUID worldId,
                                                               @AuthenticationPrincipal User user) {
        var sessions = sessionService.listSessions(worldId, user.getId())
            .stream().map(GameSessionInfoResponse::from).toList();
        return ResponseEntity.ok(sessions);
    }

    public record StartRequest(@NotNull UUID worldId, UUID campaignId) {}
}
