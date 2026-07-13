package com.lwe.api;

import com.lwe.core.domain.NpcIntent;
import com.lwe.core.domain.User;
import com.lwe.core.service.NpcIntentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/npc-intents")
public class NpcIntentController {

    private final NpcIntentService service;

    public NpcIntentController(NpcIntentService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateRequest req) {
        var intent = service.create(req.worldId(), req.npcId(), req.intentType(),
            req.paramsJson(), req.reasoning());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(intent));
    }

    @GetMapping
    public ResponseEntity<?> listPending(@RequestParam UUID worldId) {
        var intents = service.listPending(worldId).stream().map(this::toResponse).toList();
        return ResponseEntity.ok(intents);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable UUID id) {
        var intent = service.approve(id);
        return ResponseEntity.ok(toResponse(intent));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable UUID id, @RequestBody RejectRequest req) {
        var intent = service.reject(id, req.reason());
        return ResponseEntity.ok(toResponse(intent));
    }

    private Map<String, Object> toResponse(NpcIntent i) {
        return Map.of(
            "id", i.getId(),
            "world_id", i.getWorldId(),
            "npc_id", i.getNpcId(),
            "intent_type", i.getIntentType(),
            "reasoning", i.getReasoning() != null ? i.getReasoning() : "",
            "status", i.getStatus(),
            "rejection_reason", i.getRejectionReason() != null ? i.getRejectionReason() : "",
            "created_at", i.getCreatedAt().toString()
        );
    }

    public record CreateRequest(
        @NotBlank UUID worldId,
        @NotBlank UUID npcId,
        @NotBlank String intentType,
        String paramsJson,
        String reasoning
    ) {}

    public record RejectRequest(String reason) {}
}
