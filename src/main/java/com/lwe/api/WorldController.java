package com.lwe.api;

import com.lwe.core.domain.User;
import com.lwe.core.domain.World;
import com.lwe.core.service.WorldService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/worlds")
public class WorldController {

    private final WorldService worldService;

    public WorldController(WorldService worldService) {
        this.worldService = worldService;
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateRequest req,
                                    @AuthenticationPrincipal User user) {
        var world = worldService.create(req.name(), user.getId(), req.gameSystemId(), req.settingsJson());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(world));
    }

    @GetMapping
    public ResponseEntity<?> listOwned(@AuthenticationPrincipal User user) {
        var worlds = worldService.listOwned(user.getId()).stream().map(this::toResponse).toList();
        return ResponseEntity.ok(worlds);
    }

    @GetMapping("/accessible")
    public ResponseEntity<?> listAccessible(@AuthenticationPrincipal User user) {
        var worlds = worldService.listAccessible(user.getId()).stream().map(this::toResponse).toList();
        return ResponseEntity.ok(worlds);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(toResponse(worldService.getById(id, user.getId())));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @Valid @RequestBody UpdateRequest req,
                                    @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(toResponse(worldService.update(id, user.getId(), req.name(), req.settingsJson())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        worldService.delete(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<?> addMember(@PathVariable UUID id, @Valid @RequestBody MemberRequest req,
                                       @AuthenticationPrincipal User user) {
        var member = worldService.addMember(id, user.getId(), req.userId(), req.role());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "id", member.getId(),
            "user_id", member.getUserId(),
            "role", member.getRole()
        ));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<?> listMembers(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        var members = worldService.listMembers(id, user.getId());
        return ResponseEntity.ok(members.stream().map(m -> Map.of(
            "id", m.getId(),
            "user_id", m.getUserId(),
            "role", m.getRole(),
            "joined_at", m.getJoinedAt().toString()
        )).toList());
    }

    @DeleteMapping("/{id}/members/{memberId}")
    public ResponseEntity<?> removeMember(@PathVariable UUID id, @PathVariable UUID memberId,
                                          @AuthenticationPrincipal User user) {
        worldService.removeMember(id, user.getId(), memberId);
        return ResponseEntity.noContent().build();
    }

    private Map<String, Object> toResponse(World w) {
        return Map.of(
            "id", w.getId(),
            "name", w.getName(),
            "owner_id", w.getOwnerId(),
            "game_system_id", w.getGameSystemId() != null ? w.getGameSystemId() : "",
            "settings_json", w.getSettingsJson(),
            "current_game_time", w.getCurrentGameTime() != null ? w.getCurrentGameTime().toString() : "",
            "created_at", w.getCreatedAt().toString()
        );
    }

    public record CreateRequest(@NotBlank String name, UUID gameSystemId, String settingsJson) {}
    public record UpdateRequest(String name, String settingsJson) {}
    public record MemberRequest(@NotBlank UUID userId, @NotBlank String role) {}
}