package com.lwe.api;

import com.lwe.api.dto.*;
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
    public ResponseEntity<AdventureResponse> create(@Valid @RequestBody CreateRequest req,
                                                     @AuthenticationPrincipal User user) {
        var adv = adventureService.createAdventure(req.worldId(), user.getId(), req.name(),
            req.description(), req.locationId(), req.giverEntityId());
        return ResponseEntity.status(HttpStatus.CREATED).body(AdventureResponse.from(adv));
    }

    // -- Discovery --
    @GetMapping
    public ResponseEntity<List<AdventureResponse>> listByWorld(@RequestParam UUID worldId) {
        var list = adventureService.listByWorld(worldId).stream().map(AdventureResponse::from).toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/by-location/{locationId}")
    public ResponseEntity<List<AdventureResponse>> listByLocation(@PathVariable UUID locationId) {
        var list = adventureService.listByLocation(locationId).stream().map(AdventureResponse::from).toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/by-giver/{giverEntityId}")
    public ResponseEntity<List<AdventureResponse>> listByGiver(@PathVariable UUID giverEntityId) {
        var list = adventureService.listByGiver(giverEntityId).stream().map(AdventureResponse::from).toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdventureResponse> getById(@PathVariable UUID id) {
        var adv = adventureService.getById(id);
        return ResponseEntity.ok(AdventureResponse.from(adv));
    }

    // -- Override (Live DM) --
    @PostMapping("/{id}/override-text")
    public ResponseEntity<ApiResponse> overrideText(@PathVariable UUID id,
                                                     @RequestBody Map<String, String> body,
                                                     @AuthenticationPrincipal User user) {
        adventureService.overrideNodeText(id, user.getId(), body.get("text"));
        return ResponseEntity.ok(new ApiResponse("Text updated"));
    }

    @PostMapping("/{id}/force-node/{nodeId}")
    public ResponseEntity<ApiResponse> forceNode(@PathVariable UUID id,
                                                   @PathVariable UUID nodeId,
                                                   @AuthenticationPrincipal User user) {
        adventureService.forceNode(id, user.getId(), nodeId);
        return ResponseEntity.ok(new ApiResponse("Node forced"));
    }

    @PostMapping("/{id}/inject-choice/{nodeId}")
    public ResponseEntity<AdventureChoiceResponse> injectChoice(@PathVariable UUID id,
                                                                  @PathVariable UUID nodeId,
                                                                  @Valid @RequestBody ChoiceRequest req,
                                                                  @AuthenticationPrincipal User user) {
        var choice = adventureService.injectChoice(id, user.getId(), nodeId, req.label(),
            req.targetNodeId(), req.skillCheckJson());
        return ResponseEntity.status(HttpStatus.CREATED).body(AdventureChoiceResponse.created(choice));
    }

    // -- Existing endpoints --
    public ResponseEntity<AdventureNodeResponse> addNode(@PathVariable UUID id,
                                                          @Valid @RequestBody NodeRequest req,
                                                          @AuthenticationPrincipal User user) {
        var node = adventureService.addNode(id, user.getId(), req.text(), req.imageUrl(), req.isEnd());
        return ResponseEntity.status(HttpStatus.CREATED).body(AdventureNodeResponse.from(node));
    }

    @PostMapping("/{id}/start-node/{nodeId}")
    public ResponseEntity<StartNodeResponse> setStartNode(@PathVariable UUID id,
                                                           @PathVariable UUID nodeId,
                                                           @AuthenticationPrincipal User user) {
        adventureService.setStartNode(id, user.getId(), nodeId);
        return ResponseEntity.ok(new StartNodeResponse(nodeId));
    }

    @PostMapping("/{id}/nodes/{nodeId}/choices")
    public ResponseEntity<AdventureChoiceResponse> addChoice(@PathVariable UUID id,
                                                              @PathVariable UUID nodeId,
                                                              @Valid @RequestBody ChoiceRequest req,
                                                              @AuthenticationPrincipal User user) {
        var choice = adventureService.addChoice(nodeId, user.getId(), req.label(),
            req.targetNodeId(), req.skillCheckJson(), req.onSuccessNodeId(), req.onFailureNodeId());
        return ResponseEntity.status(HttpStatus.CREATED).body(AdventureChoiceResponse.created(choice));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<AdventureStartResponse> start(@PathVariable UUID id,
                                                         @RequestBody StartRequest req,
                                                         @AuthenticationPrincipal User user) {
        var progress = adventureService.start(id, req.entityId(), user.getId());
        var node = adventureService.getNodeById(progress.getCurrentNodeId(), user.getId());
        return ResponseEntity.ok(new AdventureStartResponse(
            progress.getId(), progress.getCurrentNodeId(), progress.getStatus(),
            AdventureNodeResponse.minimal(node)));
    }

    @PostMapping("/{id}/advance")
    public ResponseEntity<AdventureAdvanceResponse> advance(@PathVariable UUID id,
                                                             @RequestBody AdvanceRequest req,
                                                             @AuthenticationPrincipal User user) {
        var result = adventureService.advance(id, req.entityId(), req.choiceId(), user.getId());
        return ResponseEntity.ok(new AdventureAdvanceResponse(
            result.nextNode().getId(), result.completed(),
            result.skillCheckSuccess(), result.nextNode().getText(), result.nextNode().isEnd()));
    }

    @GetMapping("/{id}/progress/{entityId}")
    public ResponseEntity<AdventureProgressResponse> getProgress(@PathVariable UUID id,
                                                                  @PathVariable UUID entityId,
                                                                  @AuthenticationPrincipal User user) {
        var progress = adventureService.getProgress(id, entityId, user.getId());
        var node = adventureService.getNodeById(progress.getCurrentNodeId(), user.getId());
        return ResponseEntity.ok(new AdventureProgressResponse(
            progress.getStatus(), progress.getCurrentNodeId(),
            progress.getVisitedNodes(), node.getText(), node.isEnd()));
    }

    @PostMapping("/{id}/abandon")
    public ResponseEntity<AdventureStatusResponse> abandon(@PathVariable UUID id,
                                                            @RequestBody StartRequest req,
                                                            @AuthenticationPrincipal User user) {
        adventureService.abandon(id, req.entityId(), user.getId());
        return ResponseEntity.ok(new AdventureStatusResponse("abandoned"));
    }

    @GetMapping("/{id}/nodes")
    public ResponseEntity<List<AdventureNodeResponse>> getNodes(@PathVariable UUID id,
                                                                 @AuthenticationPrincipal User user) {
        var nodes = adventureService.getNodes(id, user.getId()).stream()
            .map(AdventureNodeResponse::minimal).toList();
        return ResponseEntity.ok(nodes);
    }

    @GetMapping("/{id}/nodes/{nodeId}/choices")
    public ResponseEntity<List<AdventureChoiceResponse>> getChoices(@PathVariable UUID id,
                                                                     @PathVariable UUID nodeId,
                                                                     @AuthenticationPrincipal User user) {
        var choices = adventureService.getChoices(nodeId, user.getId()).stream()
            .map(AdventureChoiceResponse::from).toList();
        return ResponseEntity.ok(choices);
    }

    @GetMapping("/{id}/nodes/{nodeId}")
    public ResponseEntity<AdventureNodeResponse> getNode(@PathVariable UUID id,
                                                          @PathVariable UUID nodeId,
                                                          @AuthenticationPrincipal User user) {
        var node = adventureService.getNodeById(nodeId, user.getId());
        return ResponseEntity.ok(AdventureNodeResponse.from(node));
    }

    public record CreateRequest(@NotBlank UUID worldId, @NotBlank String name, String description,
                                 UUID locationId, UUID giverEntityId) {}
    public record NodeRequest(@NotBlank String text, String imageUrl, boolean isEnd) {}
    public record ChoiceRequest(@NotBlank String label, UUID targetNodeId,
                                String skillCheckJson, UUID onSuccessNodeId, UUID onFailureNodeId) {}
    public record StartRequest(@NotBlank UUID entityId) {}
    public record AdvanceRequest(@NotBlank UUID entityId, @NotBlank UUID choiceId) {}
    public record StartNodeResponse(UUID startNodeId) {}
}
