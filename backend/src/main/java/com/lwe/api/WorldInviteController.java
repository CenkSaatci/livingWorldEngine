package com.lwe.api;

import com.lwe.core.domain.User;
import com.lwe.core.domain.WorldInvite;
import com.lwe.core.service.WorldInviteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/worlds")
public class WorldInviteController {

    private final WorldInviteService service;

    public WorldInviteController(WorldInviteService service) {
        this.service = service;
    }

    @PostMapping("/{worldId}/invites")
    public ResponseEntity<?> createInvite(@PathVariable UUID worldId,
                                           @RequestBody(required = false) CreateInviteRequest req,
                                           @AuthenticationPrincipal User user) {
        var maxUses = req != null ? req.maxUses : 1;
        var expiresAt = req != null ? req.expiresAt : null;
        var invite = service.create(worldId, user.getId(), maxUses, expiresAt);
        var baseUrl = System.getenv("APP_URL") != null ? System.getenv("APP_URL") : "http://localhost:5173";
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "id", invite.getId(),
            "token", invite.getToken(),
            "url", baseUrl + "/join?token=" + invite.getToken(),
            "max_uses", invite.getMaxUses(),
            "use_count", invite.getUseCount(),
            "expires_at", invite.getExpiresAt() != null ? invite.getExpiresAt().toString() : null
        ));
    }

    @GetMapping("/{worldId}/invites")
    public ResponseEntity<List<WorldInvite>> listInvites(@PathVariable UUID worldId,
                                                          @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.listByWorld(worldId, user.getId()));
    }

    @DeleteMapping("/{worldId}/invites/{inviteId}")
    public ResponseEntity<Void> deleteInvite(@PathVariable UUID worldId,
                                              @PathVariable UUID inviteId,
                                              @AuthenticationPrincipal User user) {
        service.deleteInvite(inviteId, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/join")
    public ResponseEntity<?> join(@RequestParam String token,
                                   @AuthenticationPrincipal User user) {
        service.join(token, user.getId());
        return ResponseEntity.ok(Map.of("message", "Joined world successfully"));
    }

    public record CreateInviteRequest(int maxUses, Instant expiresAt) {}
}
