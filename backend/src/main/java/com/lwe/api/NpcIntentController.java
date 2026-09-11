package com.lwe.api;

import com.lwe.api.dto.NpcIntentResponse;
import com.lwe.core.domain.User;
import com.lwe.core.service.NpcIntentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/npc-intents")
public class NpcIntentController {

    private final NpcIntentService service;

    public NpcIntentController(NpcIntentService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<NpcIntentResponse> create(@Valid @RequestBody CreateRequest req,
                                                     @AuthenticationPrincipal User user) {
        var intent = service.create(req.worldId(), req.campaignId(), req.npcId(), req.intentType(),
            req.paramsJson(), req.reasoning());
        return ResponseEntity.status(HttpStatus.CREATED).body(NpcIntentResponse.from(intent));
    }

    @GetMapping
    public ResponseEntity<List<NpcIntentResponse>> listPending(@RequestParam UUID worldId,
                                                                @AuthenticationPrincipal User user) {
        var intents = service.listPending(worldId, user.getId()).stream().map(NpcIntentResponse::from).toList();
        return ResponseEntity.ok(intents);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<NpcIntentResponse> approve(@PathVariable UUID id,
                                                      @AuthenticationPrincipal User user) {
        var intent = service.approve(id, user.getId());
        return ResponseEntity.ok(NpcIntentResponse.from(intent));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<NpcIntentResponse> reject(@PathVariable UUID id,
                                                     @RequestBody RejectRequest req,
                                                     @AuthenticationPrincipal User user) {
        var intent = service.reject(id, req.reason(), user.getId());
        return ResponseEntity.ok(NpcIntentResponse.from(intent));
    }

    public record CreateRequest(
        @NotNull UUID worldId, UUID campaignId, @NotNull UUID npcId, @NotBlank String intentType,
        String paramsJson, String reasoning
    ) {}
    public record RejectRequest(String reason) {}
}
