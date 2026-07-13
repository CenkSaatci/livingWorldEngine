package com.lwe.api;

import com.lwe.core.domain.Quest;
import com.lwe.core.domain.User;
import com.lwe.core.service.QuestService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/quests")
public class QuestController {

    private final QuestService service;

    public QuestController(QuestService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateRequest req,
                                    @AuthenticationPrincipal User user) {
        var quest = service.create(req.worldId(), user.getId(), req.title(),
            req.description(), req.type(), req.giverId(), req.locationId(),
            req.objectives(), req.rewards(), req.aiGenerated());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(quest));
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam UUID worldId,
                                  @RequestParam(required = false) String status,
                                  @AuthenticationPrincipal User user) {
        var list = service.list(worldId, user.getId(), status).stream().map(this::toResponse).toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{questId}")
    public ResponseEntity<?> getById(@PathVariable UUID questId,
                                     @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(toResponse(service.getById(questId, user.getId())));
    }

    @PatchMapping("/{questId}/status")
    public ResponseEntity<?> updateStatus(@PathVariable UUID questId,
                                          @RequestBody StatusRequest req,
                                          @AuthenticationPrincipal User user) {
        var quest = service.updateStatus(questId, user.getId(), req.status());
        return ResponseEntity.ok(toResponse(quest));
    }

    @DeleteMapping("/{questId}")
    public ResponseEntity<?> delete(@PathVariable UUID questId,
                                    @AuthenticationPrincipal User user) {
        service.delete(questId, user.getId());
        return ResponseEntity.noContent().build();
    }

    private Map<String, Object> toResponse(Quest q) {
        var m = new HashMap<String, Object>();
        m.put("id", q.getId());
        m.put("world_id", q.getWorldId());
        m.put("title", q.getTitle());
        m.put("description", q.getDescription() != null ? q.getDescription() : "");
        m.put("type", q.getType());
        m.put("status", q.getStatus());
        m.put("objectives", q.getObjectives());
        m.put("rewards", q.getRewards());
        m.put("ai_generated", q.isAiGenerated());
        m.put("created_at", q.getCreatedAt().toString());
        return m;
    }

    public record CreateRequest(
        @NotBlank UUID worldId, @NotBlank String title, String description,
        @NotBlank String type, UUID giverId, UUID locationId,
        String objectives, String rewards, boolean aiGenerated
    ) {}
    public record StatusRequest(@NotBlank String status) {}
}
