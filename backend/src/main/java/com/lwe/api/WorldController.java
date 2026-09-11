package com.lwe.api;

import com.lwe.api.dto.PaginatedWorldResponse;
import com.lwe.api.dto.WorldInfoResponse;
import com.lwe.api.dto.WorldMemberResponse;
import com.lwe.core.domain.User;
import com.lwe.core.service.WorldService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/worlds")
public class WorldController {

    private final WorldService worldService;

    public WorldController(WorldService worldService) {
        this.worldService = worldService;
    }

    @PostMapping
    public ResponseEntity<WorldInfoResponse> create(@Valid @RequestBody CreateRequest req,
                                                     @AuthenticationPrincipal User user) {
        var world = worldService.create(req.name(), user.getId(), req.settingsJson(), user);
        return ResponseEntity.status(HttpStatus.CREATED).body(WorldInfoResponse.from(world));
    }

    @GetMapping
    public ResponseEntity<List<WorldInfoResponse>> listOwned(@AuthenticationPrincipal User user) {
        var worlds = worldService.listOwned(user.getId()).stream().map(WorldInfoResponse::from).toList();
        return ResponseEntity.ok(worlds);
    }

    @GetMapping("/accessible")
    public ResponseEntity<PaginatedWorldResponse> listAccessible(@AuthenticationPrincipal User user,
                                                                   @RequestParam(defaultValue = "0") @Min(0) int page,
                                                                   @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        var result = worldService.listAccessible(user.getId(), page, size);
        return ResponseEntity.ok(new PaginatedWorldResponse(
            result.items().stream().map(WorldInfoResponse::from).toList(),
            result.total(), result.page(), result.hasMore()
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorldInfoResponse> getById(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(WorldInfoResponse.from(worldService.getById(id, user.getId())));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<WorldInfoResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateRequest req,
                                                     @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(WorldInfoResponse.from(
            worldService.update(id, user.getId(), req.name(), req.settingsJson())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        worldService.delete(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/clone")
    public ResponseEntity<WorldInfoResponse> clone(@PathVariable UUID id,
                                                    @AuthenticationPrincipal User user) {
        var clone = worldService.clone(id, user.getId(), user);
        return ResponseEntity.status(HttpStatus.CREATED).body(WorldInfoResponse.from(clone));
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<WorldMemberResponse> addMember(@PathVariable UUID id,
                                                          @Valid @RequestBody MemberRequest req,
                                                          @AuthenticationPrincipal User user) {
        var member = worldService.addMember(id, user.getId(), req.userId(), req.role());
        return ResponseEntity.status(HttpStatus.CREATED).body(WorldMemberResponse.from(member));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<WorldMemberResponse>> listMembers(@PathVariable UUID id,
                                                                  @AuthenticationPrincipal User user) {
        var members = worldService.listMembers(id, user.getId());
        return ResponseEntity.ok(members.stream().map(WorldMemberResponse::from).toList());
    }

    @DeleteMapping("/{id}/members/{memberId}")
    public ResponseEntity<Void> removeMember(@PathVariable UUID id, @PathVariable UUID memberId,
                                              @AuthenticationPrincipal User user) {
        worldService.removeMember(id, user.getId(), memberId);
        return ResponseEntity.noContent().build();
    }

    // Entfernte Felder (z.B. gameSystemId, P25-T06) werden ignoriert statt
    // 500 zu werfen — alte Clients bleiben kompatibel.
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public record CreateRequest(@NotBlank String name, String settingsJson) {}
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public record UpdateRequest(String name, String settingsJson) {}
    public record MemberRequest(@NotNull UUID userId, @NotBlank String role) {}
}
