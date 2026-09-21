package com.lwe.api;

import com.lwe.core.domain.User;
import com.lwe.core.service.PoiActionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * POI-Aktionen (ADR-015): Katalog aus dem Regelwerk, gebunden an Ort/NPC.
 */
@RestController
@RequestMapping("/api/v1/locations/{locationId}")
public class PoiActionController {

    private final PoiActionService service;

    public PoiActionController(PoiActionService service) {
        this.service = service;
    }

    @GetMapping("/actions")
    public ResponseEntity<List<PoiActionService.ActionInfo>> list(
            @PathVariable UUID locationId,
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) UUID campaignId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.list(locationId, actorId, user.getId(), campaignId));
    }

    @PostMapping("/actions/{actionName}")
    public ResponseEntity<PoiActionService.ActionResult> execute(
            @PathVariable UUID locationId,
            @PathVariable String actionName,
            @RequestBody ExecuteRequest req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.execute(locationId, actionName, req.actorId(),
            user.getId(), req.campaignId()));
    }

    public record ExecuteRequest(UUID actorId, UUID campaignId) {}
}
