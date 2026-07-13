package com.lwe.api;

import com.lwe.core.domain.User;
import com.lwe.core.service.InventoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/entities/{entityId}/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public ResponseEntity<?> getInventory(@PathVariable UUID entityId,
                                          @AuthenticationPrincipal User user) {
        var result = inventoryService.getInventory(entityId, user.getId());
        return ResponseEntity.ok(Map.of(
            "items", result.items(),
            "computed_bonuses", result.computedBonuses()
        ));
    }

    @PostMapping("/add")
    public ResponseEntity<?> addItem(@PathVariable UUID entityId,
                                     @RequestBody AddRequest req,
                                     @AuthenticationPrincipal User user) {
        inventoryService.addItem(entityId, user.getId(), req.itemId(), req.quantity());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("added", true));
    }

    @PostMapping("/remove")
    public ResponseEntity<?> removeItem(@PathVariable UUID entityId,
                                        @RequestBody RemoveRequest req,
                                        @AuthenticationPrincipal User user) {
        inventoryService.removeItem(entityId, user.getId(), req.itemId(), req.quantity());
        return ResponseEntity.ok(Map.of("removed", true));
    }

    @PostMapping("/equip")
    public ResponseEntity<?> equipItem(@PathVariable UUID entityId,
                                       @RequestBody EquipRequest req,
                                       @AuthenticationPrincipal User user) {
        var result = inventoryService.equipItem(entityId, user.getId(), req.itemId(), req.slot());
        return ResponseEntity.ok(Map.of(
            "items", result.items(),
            "computed_bonuses", result.computedBonuses()
        ));
    }

    @PostMapping("/unequip")
    public ResponseEntity<?> unequipItem(@PathVariable UUID entityId,
                                         @RequestBody UnequipRequest req,
                                         @AuthenticationPrincipal User user) {
        var result = inventoryService.unequipItem(entityId, user.getId(), req.slot());
        return ResponseEntity.ok(Map.of(
            "items", result.items(),
            "computed_bonuses", result.computedBonuses()
        ));
    }

    public record AddRequest(UUID itemId, int quantity) {}
    public record RemoveRequest(UUID itemId, int quantity) {}
    public record EquipRequest(UUID itemId, String slot) {}
    public record UnequipRequest(String slot) {}
}