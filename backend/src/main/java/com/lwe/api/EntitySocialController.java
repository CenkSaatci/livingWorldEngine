package com.lwe.api;

import com.lwe.api.dto.MemoryIdResponse;
import com.lwe.core.domain.User;
import com.lwe.core.service.MemoryService;
import com.lwe.core.service.RelationshipService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/entities/{entityId}")
public class EntitySocialController {

    private final MemoryService memoryService;
    private final RelationshipService relationshipService;

    public EntitySocialController(MemoryService memoryService,
                                   RelationshipService relationshipService) {
        this.memoryService = memoryService;
        this.relationshipService = relationshipService;
    }

    @GetMapping("/memories")
    public ResponseEntity<?> getMemories(@PathVariable UUID entityId) {
        return ResponseEntity.ok(memoryService.getMemories(entityId));
    }

    @PostMapping("/memories")
    public ResponseEntity<MemoryIdResponse> addMemory(@PathVariable UUID entityId,
                                                       @RequestBody MemoryRequest req) {
        var mem = memoryService.addMemory(entityId, req.subjectId(), req.memoryType(),
            req.sentiment(), req.summary(), req.sourceEventId());
        return ResponseEntity.ok(new MemoryIdResponse(mem.getId()));
    }

    @GetMapping("/relationships")
    public ResponseEntity<?> getRelationships(@PathVariable UUID entityId) {
        return ResponseEntity.ok(relationshipService.getRelationships(entityId));
    }

    @PostMapping("/relationships")
    public ResponseEntity<MemoryIdResponse> setRelationship(@PathVariable UUID entityId,
                                                             @RequestBody RelationRequest req) {
        var rel = relationshipService.setRelationship(entityId, req.otherId(), req.relationship());
        return ResponseEntity.ok(new MemoryIdResponse(rel.getId()));
    }

    public record MemoryRequest(UUID subjectId, String memoryType, int sentiment,
                                String summary, Long sourceEventId) {}
    public record RelationRequest(UUID otherId, String relationship) {}
}
