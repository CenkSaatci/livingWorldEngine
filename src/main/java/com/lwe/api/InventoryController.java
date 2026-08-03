package com.lwe.api;

import com.lwe.api.dto.ApiResponse;
import com.lwe.api.dto.InventoryResponse;
import com.lwe.core.domain.User;
import com.lwe.core.service.InventoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/entities/{entityId}/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public ResponseEntity<InventoryResponse> getInventory(@PathVariable UUID entityId,
                                                           @AuthenticationPrincipal User user) {
        var result = inventoryService.getInventory(entityId, user.getId());
        return ResponseEntity.ok(new InventoryResponse(result.items(), result.computedBonuses()));
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse> addItem(@PathVariable UUID entityId,
                                                @RequestBody AddRequest req,
                                                @AuthenticationPrincipal User user) {
        inventoryService.addItem(entityId, user.getId(), req.itemId(), req.quantity());
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse("added"));
    }

    @PostMapping("/remove")
    public ResponseEntity<ApiResponse> removeItem(@PathVariable UUID entityId,
                                                   @RequestBody RemoveRequest req,
                                                   @AuthenticationPrincipal User user) {
        inventoryService.removeItem(entityId, user.getId(), req.itemId(), req.quantity());
        return ResponseEntity.ok(new ApiResponse("removed"));
    }

    @PostMapping("/equip")
    public ResponseEntity<InventoryResponse> equipItem(@PathVariable UUID entityId,
                                                        @RequestBody EquipRequest req,
                                                        @AuthenticationPrincipal User user) {
        var result = inventoryService.equipItem(entityId, user.getId(), req.itemId(), req.slot());
        return ResponseEntity.ok(new InventoryResponse(result.items(), result.computedBonuses()));
    }

    @PostMapping("/unequip")
    public ResponseEntity<InventoryResponse> unequipItem(@PathVariable UUID entityId,
                                                          @RequestBody UnequipItemRequest req,
                                                          @AuthenticationPrincipal User user) {
        var result = inventoryService.unequipItem(entityId, user.getId(), req.itemId());
        return ResponseEntity.ok(new InventoryResponse(result.items(), result.computedBonuses()));
    }

    @PostMapping("/use/{itemId}")
    public ResponseEntity<ApiResponse> useItem(@PathVariable UUID entityId,
                                                @PathVariable UUID itemId,
                                                @AuthenticationPrincipal User user) {
        inventoryService.useConsumable(entityId, user.getId(), itemId);
        return ResponseEntity.ok(new ApiResponse("used"));
    }

    public record AddRequest(UUID itemId, int quantity) {}
    public record RemoveRequest(UUID itemId, int quantity) {}
    public record EquipRequest(UUID itemId, String slot) {}
    public record UnequipItemRequest(UUID itemId) {}
}
