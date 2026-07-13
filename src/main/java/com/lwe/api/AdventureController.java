package com.lwe.api;

import com.lwe.core.domain.Adventure;
import com.lwe.core.domain.AdventureNode;
import com.lwe.core.domain.User;
import com.lwe.core.service.AdventureService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/adventures")
public class AdventureController {

    private final AdventureService adventureService;

    public AdventureController(AdventureService adventureService) {
        this.adventureService = adventureService;
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateRequest req,
                                    @AuthenticationPrincipal User user) {
        var adv = adventureService.createAdventure(req.worldId(), user.getId(), req.name(), req.description());
        return ResponseEntity.status(HttpStatus.CREATED).body(advResponse(adv));
    }

    @PostMapping("/{id}/nodes")
    public ResponseEntity<?> addNode(@PathVariable UUID id, @Valid @RequestBody NodeRequest req,
                                     @AuthenticationPrincipal User user) {
        var node = adventureService.addNode(id, user.getId(), req.text(), req.imageUrl(), req.isEnd());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "id", node.getId(),
            "adventure_id", node.getAdventureId(),
            "text", node.getText(),
            "image_url", node.getImageUrl() != null ? node.getImageUrl() : "",
            "is_end", node.isEnd()
        ));
    }

    @PostMapping("/{id}/start-node/{nodeId}")
    public ResponseEntity<?> setStartNode(@PathVariable UUID id, @PathVariable UUID nodeId,
                                          @AuthenticationPrincipal User user) {
        adventureService.setStartNode(id, user.getId(), nodeId);
        return ResponseEntity.ok(Map.of("start_node_id", nodeId));
    }

    @PostMapping("/{id}/nodes/{nodeId}/choices")
    public ResponseEntity<?> addChoice(@PathVariable UUID id, @PathVariable UUID nodeId,
                                       @Valid @RequestBody ChoiceRequest req,
                                       @AuthenticationPrincipal User user) {
        var choice = adventureService.addChoice(nodeId, user.getId(), req.label(),
            req.targetNodeId(), req.skillCheckJson(), req.onSuccessNodeId(), req.onFailureNodeId());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "id", choice.getId(),
            "label", choice.getLabel()
        ));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<?> start(@PathVariable UUID id, @RequestBody StartRequest req,
                                   @AuthenticationPrincipal User user) {
        var progress = adventureService.start(id, req.entityId(), user.getId());
        var node = adventureService.getNodeById(progress.getCurrentNodeId(), user.getId());
        return ResponseEntity.ok(Map.of(
            "progress_id", progress.getId(),
            "current_node_id", progress.getCurrentNodeId(),
            "status", progress.getStatus(),
            "node", node
        ));
    }

    @PostMapping("/{id}/advance")
    public ResponseEntity<?> advance(@PathVariable UUID id, @RequestBody AdvanceRequest req,
                                     @AuthenticationPrincipal User user) {
        var result = adventureService.advance(id, req.entityId(), req.choiceId(), user.getId());
        return ResponseEntity.ok(Map.of(
            "next_node_id", result.nextNode().getId(),
            "completed", result.completed(),
            "skill_check_success", result.skillCheckSuccess(),
            "node_text", result.nextNode().getText(),
            "is_end", result.nextNode().isEnd()
        ));
    }

    private Map<String, Object> advResponse(Adventure a) {
        return Map.of("id", a.getId(), "world_id", a.getWorldId(), "name", a.getName(),
            "description", a.getDescription() != null ? a.getDescription() : "",
            "start_node_id", a.getStartNodeId() != null ? a.getStartNodeId().toString() : "",
            "created_at", a.getCreatedAt().toString());
    }

    public record CreateRequest(@NotBlank UUID worldId, @NotBlank String name, String description) {}
    public record NodeRequest(@NotBlank String text, String imageUrl, boolean isEnd) {}
    public record ChoiceRequest(@NotBlank String label, UUID targetNodeId,
                                String skillCheckJson, UUID onSuccessNodeId, UUID onFailureNodeId) {}
    public record StartRequest(@NotBlank UUID entityId) {}
    public record AdvanceRequest(@NotBlank UUID entityId, @NotBlank UUID choiceId) {}

    @GetMapping("/{id}/progress/{entityId}")
    public ResponseEntity<?> getProgress(@PathVariable UUID id, @PathVariable UUID entityId,
                                         @AuthenticationPrincipal User user) {
        var progress = adventureService.getProgress(id, entityId, user.getId());
        var node = adventureService.getNodeById(progress.getCurrentNodeId(), user.getId());
        return ResponseEntity.ok(Map.of(
            "status", progress.getStatus(),
            "current_node_id", progress.getCurrentNodeId(),
            "visited_nodes", progress.getVisitedNodes(),
            "node_text", node.getText(),
            "is_end", node.isEnd()
        ));
    }

    @PostMapping("/{id}/abandon")
    public ResponseEntity<?> abandon(@PathVariable UUID id, @RequestBody StartRequest req,
                                     @AuthenticationPrincipal User user) {
        adventureService.abandon(id, req.entityId(), user.getId());
        return ResponseEntity.ok(Map.of("status", "abandoned"));
    }

    @GetMapping("/{id}/nodes")
    public ResponseEntity<?> getNodes(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        var nodes = adventureService.getNodes(id, user.getId()).stream()
            .map(n -> Map.of("id", n.getId(), "text", n.getText(), "is_end", n.isEnd()))
            .toList();
        return ResponseEntity.ok(nodes);
    }

    @GetMapping("/{id}/nodes/{nodeId}/choices")
    public ResponseEntity<?> getChoices(@PathVariable UUID id, @PathVariable UUID nodeId,
                                        @AuthenticationPrincipal User user) {
        var choices = adventureService.getChoices(nodeId, user.getId()).stream()
            .map(c -> Map.of("id", c.getId(), "label", c.getLabel(), "skill_check",
                c.getSkillCheck() != null ? c.getSkillCheck() : ""))
            .toList();
        return ResponseEntity.ok(choices);
    }

    @GetMapping("/{id}/nodes/{nodeId}")
    public ResponseEntity<?> getNode(@PathVariable UUID id, @PathVariable UUID nodeId,
                                     @AuthenticationPrincipal User user) {
        var node = adventureService.getNodeById(nodeId, user.getId());
        return ResponseEntity.ok(Map.of(
            "id", node.getId(), "text", node.getText(),
            "image_url", node.getImageUrl() != null ? node.getImageUrl() : "",
            "is_end", node.isEnd()
        ));
    }
}