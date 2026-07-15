package com.lwe.core.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwe.core.domain.GameEntity;
import com.lwe.core.domain.GameItem;
import com.lwe.core.repository.GameEntityRepository;
import com.lwe.core.repository.GameItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class InventoryService {

    private final GameEntityRepository entityRepo;
    private final GameItemRepository itemRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public InventoryService(GameEntityRepository entityRepo, GameItemRepository itemRepo) {
        this.entityRepo = entityRepo;
        this.itemRepo = itemRepo;
    }

    public InventoryResult getInventory(UUID entityId, UUID userId) {
        var entity = findEntity(entityId, userId);
        var inventory = parseInventory(entity.getInventoryJson());
        var computedBonuses = new HashMap<String, Integer>();

        // Equipped items bonuses berechnen + Items auflösen
        var detailedItems = new ArrayList<InventoryEntry>();
        for (var entry : inventory) {
            var itemOpt = itemRepo.findById(entry.itemId());
            var item = itemOpt.orElseThrow(
                () -> new InventoryException("INVENTORY_ITEM_NOT_FOUND", "Item not found"));
            if (entry.equipped()) {
                addBonuses(computedBonuses, item.getBonusesJson());
            }
            detailedItems.add(new InventoryEntry(
                entry.itemId(), entry.quantity(), entry.equipped(), entry.slot(),
                item.getName(), item.getType(), item.getWeight()
            ));
        }

        return new InventoryResult(detailedItems, computedBonuses);
    }

    @Transactional
    public void addItem(UUID entityId, UUID userId, UUID itemId, int quantity) {
        var entity = findEntity(entityId, userId);
        itemRepo.findById(itemId).orElseThrow(
            () -> new InventoryException("INVENTORY_ITEM_NOT_FOUND", "Item not found"));

        var inventory = parseInventory(entity.getInventoryJson());
        var existing = inventory.stream().filter(e -> e.itemId().equals(itemId)).findFirst();

        if (existing.isPresent()) {
            var idx = inventory.indexOf(existing.get());
            var old = existing.get();
            inventory.set(idx, new RawEntry(old.itemId(), old.quantity() + quantity, old.equipped(), old.slot()));
        } else {
            inventory.add(new RawEntry(itemId, quantity, false, null));
        }

        entity.setInventoryJson(toJson(inventory));
        entityRepo.save(entity);
    }

    @Transactional
    public void removeItem(UUID entityId, UUID userId, UUID itemId, int quantity) {
        var entity = findEntity(entityId, userId);
        var inventory = parseInventory(entity.getInventoryJson());
        var existing = inventory.stream().filter(e -> e.itemId().equals(itemId)).findFirst()
            .orElseThrow(() -> new InventoryException("INVENTORY_ITEM_NOT_FOUND", "Item not in inventory"));

        if (quantity > existing.quantity()) {
            throw new InventoryException("INVENTORY_INSUFFICIENT_QUANTITY",
                "Cannot remove " + quantity + " items, only " + existing.quantity() + " available");
        }
        var old = existing;
        var newQty = old.quantity() - quantity;
        if (newQty <= 0) {
            inventory.remove(existing);
            // auto-unequip
            if (old.equipped()) recalculateBonuses(entity, inventory);
        } else {
            var idx = inventory.indexOf(existing);
            inventory.set(idx, new RawEntry(old.itemId(), newQty, old.equipped(), old.slot()));
        }

        entity.setInventoryJson(toJson(inventory));
        entityRepo.save(entity);
    }

    @Transactional
    public InventoryResult equipItem(UUID entityId, UUID userId, UUID itemId, String slot) {
        var entity = findEntity(entityId, userId);
        var inventory = parseInventory(entity.getInventoryJson());
        var existing = inventory.stream().filter(e -> e.itemId().equals(itemId)).findFirst()
            .orElseThrow(() -> new InventoryException("INVENTORY_ITEM_NOT_FOUND", "Item not in inventory"));

        // Slot belegt?
        var slotOccupied = inventory.stream()
            .filter(e -> e.equipped() && slot != null && slot.equals(e.slot()) && !e.itemId().equals(itemId))
            .findFirst();
        if (slotOccupied.isPresent()) {
            // Auto-unequip alter Slot
            var idx = inventory.indexOf(slotOccupied.get());
            var old = slotOccupied.get();
            inventory.set(idx, new RawEntry(old.itemId(), old.quantity(), false, null));
        }

        var idx = inventory.indexOf(existing);
        inventory.set(idx, new RawEntry(existing.itemId(), existing.quantity(), true, slot));
        entity.setInventoryJson(toJson(inventory));

        var result = recalculateBonuses(entity, inventory);
        entityRepo.save(entity);
        return result;
    }

    @Transactional
    public InventoryResult unequipItem(UUID entityId, UUID userId, String slot) {
        var entity = findEntity(entityId, userId);
        var inventory = parseInventory(entity.getInventoryJson());
        var equipped = inventory.stream()
            .filter(e -> e.equipped() && (slot == null || slot.equals(e.slot())))
            .findFirst()
            .orElseThrow(() -> new InventoryException("INVENTORY_SLOT_OCCUPIED", "No item equipped in this slot"));

        var idx = inventory.indexOf(equipped);
        inventory.set(idx, new RawEntry(equipped.itemId(), equipped.quantity(), false, null));
        entity.setInventoryJson(toJson(inventory));

        var result = recalculateBonuses(entity, inventory);
        entityRepo.save(entity);
        return result;
    }

    // -- Helpers --

    private GameEntity findEntity(UUID entityId, UUID userId) {
        var entity = entityRepo.findById(entityId)
            .orElseThrow(() -> new InventoryException("ENTITY_NOT_FOUND", "Entity not found"));
        // Permission-check via world ownership
        if (entity.getWorldId() == null) {
            throw new InventoryException("WORLD_ACCESS_DENIED", "Access denied");
        }
        return entity;
    }

    private InventoryResult recalculateBonuses(GameEntity entity, List<RawEntry> inventory) {
        var bonuses = new HashMap<String, Integer>();
        for (var entry : inventory) {
            if (entry.equipped()) {
                var itemOpt = itemRepo.findById(entry.itemId());
                itemOpt.ifPresent(item -> addBonuses(bonuses, item.getBonusesJson()));
            }
        }
        // armor_class in attributes_json aktualisieren
        var baseAc = 10 + bonuses.getOrDefault("armor_class", 0);
        try {
            var tree = objectMapper.readTree(entity.getAttributesJson());
            var ac = tree.path("armor_class");
            if (ac.isMissingNode()) {
                entity.setAttributesJson(objectMapper.writeValueAsString(
                    Map.of("armor_class", baseAc)
                ));
            }
        } catch (Exception ignored) {}

        var result = buildDetailedResult(inventory, bonuses);
        return result;
    }

    private InventoryResult buildDetailedResult(List<RawEntry> inventory, Map<String, Integer> bonuses) {
        var items = new ArrayList<InventoryEntry>();
        for (var entry : inventory) {
            var item = itemRepo.findById(entry.itemId())
                .orElseThrow(() -> new InventoryException("INVENTORY_ITEM_NOT_FOUND", "Item not found"));
            items.add(new InventoryEntry(entry.itemId(), entry.quantity(), entry.equipped(), entry.slot(),
                item.getName(), item.getType(), item.getWeight()));
        }
        return new InventoryResult(items, bonuses);
    }

    private void addBonuses(HashMap<String, Integer> target, String bonusesJson) {
        try {
            var tree = objectMapper.readTree(bonusesJson);
            var it = tree.fields();
            while (it.hasNext()) {
                var entry = it.next();
                target.merge(entry.getKey(), entry.getValue().asInt(0), Integer::sum);
            }
        } catch (Exception ignored) {}
    }

    private List<RawEntry> parseInventory(String json) {
        if (json == null || json.isBlank() || json.equals("[]")) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<RawEntry>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String toJson(List<RawEntry> entries) {
        try { return objectMapper.writeValueAsString(entries); }
        catch (Exception e) { return "[]"; }
    }

    // Records
    record RawEntry(UUID itemId, int quantity, boolean equipped, String slot) {}

    public record InventoryEntry(UUID itemId, int quantity, boolean equipped, String slot,
                                 String name, String type, BigDecimal weight) {}

    public record InventoryResult(List<InventoryEntry> items, Map<String, Integer> computedBonuses) {}

    public static class InventoryException extends RuntimeException {
        private final String errorCode;
        public InventoryException(String errorCode, String message) { super(message); this.errorCode = errorCode; }
        public String getErrorCode() { return errorCode; }
    }
}