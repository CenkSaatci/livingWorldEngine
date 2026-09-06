package com.lwe.api;

import com.lwe.api.dto.QuestResponse;
import com.lwe.core.domain.User;
import com.lwe.core.service.QuestService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/quests")
public class QuestController {

    private final QuestService service;

    public QuestController(QuestService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<QuestResponse> create(@Valid @RequestBody CreateRequest req,
                                                 @AuthenticationPrincipal User user) {
        var quest = service.create(req.worldId(), user.getId(), req.title(),
            req.description(), req.type(), req.giverId(), req.locationId(),
            req.objectives(), req.rewards(), req.aiGenerated());
        return ResponseEntity.status(HttpStatus.CREATED).body(QuestResponse.from(quest));
    }

    @GetMapping
    public ResponseEntity<List<QuestResponse>> list(@RequestParam UUID worldId,
                                                     @RequestParam(required = false) String status,
                                                     @AuthenticationPrincipal User user) {
        var list = service.list(worldId, user.getId(), status)
            .stream().map(QuestResponse::from).toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{questId}")
    public ResponseEntity<QuestResponse> getById(@PathVariable UUID questId,
                                                  @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(QuestResponse.from(service.getById(questId, user.getId())));
    }

    @PatchMapping("/{questId}/status")
    public ResponseEntity<QuestResponse> updateStatus(@PathVariable UUID questId,
                                                       @RequestBody StatusRequest req,
                                                       @AuthenticationPrincipal User user) {
        var quest = service.updateStatus(questId, user.getId(), req.status());
        return ResponseEntity.ok(QuestResponse.from(quest));
    }

    @DeleteMapping("/{questId}")
    public ResponseEntity<Void> delete(@PathVariable UUID questId,
                                        @AuthenticationPrincipal User user) {
        service.delete(questId, user.getId());
        return ResponseEntity.noContent().build();
    }

    public record CreateRequest(
        @NotBlank UUID worldId, @NotBlank String title, String description,
        @NotBlank String type, UUID giverId, UUID locationId,
        String objectives, String rewards, boolean aiGenerated
    ) {}
    public record StatusRequest(@NotBlank String status) {}
}
