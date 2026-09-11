package com.lwe.api;

import com.lwe.core.domain.User;
import com.lwe.core.service.FactionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class FactionController {

    private final FactionService factionService;

    public FactionController(FactionService factionService) {
        this.factionService = factionService;
    }

    // -- Factions --

    @GetMapping("/worlds/{worldId}/factions")
    public ResponseEntity<?> list(@PathVariable UUID worldId,
                                  @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(factionService.list(worldId, user.getId()));
    }

    @PostMapping("/worlds/{worldId}/factions")
    public ResponseEntity<?> create(@PathVariable UUID worldId,
                                    @Valid @RequestBody CreateRequest req,
                                    @AuthenticationPrincipal User user) {
        var faction = factionService.create(worldId, user.getId(),
            req.name, req.description, req.color, req.leaderEntityId);
        return ResponseEntity.status(HttpStatus.CREATED).body(faction);
    }

    @GetMapping("/factions/{factionId}")
    public ResponseEntity<?> get(@PathVariable UUID factionId,
                                 @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(factionService.getById(factionId, user.getId()));
    }

    @PutMapping("/factions/{factionId}")
    public ResponseEntity<?> update(@PathVariable UUID factionId,
                                    @Valid @RequestBody CreateRequest req,
                                    @AuthenticationPrincipal User user) {
        var faction = factionService.update(factionId, user.getId(),
            req.name, req.description, req.color, req.leaderEntityId);
        return ResponseEntity.ok(faction);
    }

    @DeleteMapping("/factions/{factionId}")
    public ResponseEntity<?> delete(@PathVariable UUID factionId,
                                    @AuthenticationPrincipal User user) {
        factionService.delete(factionId, user.getId());
        return ResponseEntity.noContent().build();
    }

    // -- Diplomatie --

    @GetMapping("/factions/{factionId}/relations")
    public ResponseEntity<?> getRelations(@PathVariable UUID factionId,
                                          @AuthenticationPrincipal User user) {
        factionService.getById(factionId, user.getId()); // ownership check
        return ResponseEntity.ok(factionService.getRelations(factionId));
    }

    @PostMapping("/factions/{factionAId}/relations/{factionBId}")
    public ResponseEntity<?> setRelation(@PathVariable UUID factionAId,
                                          @PathVariable UUID factionBId,
                                          @Valid @RequestBody RelationRequest req,
                                          @AuthenticationPrincipal User user) {
        var relation = factionService.setRelation(factionAId, factionBId, req.status, user.getId());
        return ResponseEntity.ok(relation);
    }

    // -- Records --

    public record CreateRequest(@NotBlank String name, String description, String color, UUID leaderEntityId) {}
    public record RelationRequest(@NotBlank String status) {}
}
