package com.lwe.api;

import com.lwe.core.domain.User;
import com.lwe.core.service.MerchantService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Händler (ADR-015): NPCs mit Sortiment; Kauf/Verkauf atomar.
 */
@RestController
@RequestMapping("/api/v1")
public class MerchantController {

    private final MerchantService service;

    public MerchantController(MerchantService service) {
        this.service = service;
    }

    @GetMapping("/locations/{locationId}/merchants")
    public ResponseEntity<List<MerchantService.MerchantInfo>> list(
            @PathVariable UUID locationId,
            @RequestParam(required = false) UUID campaignId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.list(locationId, user.getId(), campaignId));
    }

    @PostMapping("/merchants/{npcId}/buy")
    public ResponseEntity<MerchantService.TradeResult> buy(
            @PathVariable UUID npcId,
            @RequestBody TradeRequest req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.buy(req.locationId(), npcId, req.actorId(),
            user.getId(), req.item(), req.qty(), req.campaignId()));
    }

    @PostMapping("/merchants/{npcId}/sell")
    public ResponseEntity<MerchantService.TradeResult> sell(
            @PathVariable UUID npcId,
            @RequestBody TradeRequest req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.sell(req.locationId(), npcId, req.actorId(),
            user.getId(), req.item(), req.qty(), req.campaignId()));
    }

    public record TradeRequest(UUID locationId, UUID actorId, String item, int qty, UUID campaignId) {}
}
