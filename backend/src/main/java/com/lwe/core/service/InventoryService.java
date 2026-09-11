package com.lwe.core.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwe.core.domain.GameEntity;
import com.lwe.core.domain.GameItem;
import com.lwe.core.repository.GameEntityRepository;
import com.lwe.core.repository.GameItemRepository;
import com.lwe.core.util.WorldAccess;
import com.lwe.rules.DiceExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class InventoryService {

    private final GameEntityRepository entityRepo;
    private final GameItemRepository itemRepo;
    private final WorldAccess worldAccess;
    private final ObjectMapper objectMapper;

    public InventoryService(GameEntityRepository entityRepo, GameItemRepository itemRepo,
                            WorldAccess worldAccess,
                        ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.entityRepo = entityRepo;
        this.itemRepo = itemRepo;
        this.worldAccess = worldAccess;
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
                item.getName(), item.getType(), item.getWeight(),
                damageTypeOf(item.getMetadataJson())
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
    public InventoryResult unequipItem(UUID entityId, UUID userId, UUID itemId) {
        var entity = findEntity(entityId, userId);
        var inventory = parseInventory(entity.getInventoryJson());
        var equipped = inventory.stream()
            .filter(e -> e.itemId().equals(itemId) && e.equipped())
            .findFirst()
            .orElseThrow(() -> new InventoryException("INVENTORY_ITEM_NOT_FOUND", "Item not equipped"));

        var idx = inventory.indexOf(equipped);
        inventory.set(idx, new RawEntry(equipped.itemId(), equipped.quantity(), false, null));
        entity.setInventoryJson(toJson(inventory));

        var result = recalculateBonuses(entity, inventory);
        entityRepo.save(entity);
        return result;
    }

    // -- Helpers --

    @Transactional
    public int useConsumable(UUID entityId, UUID userId, UUID itemId) {
        var entity = findEntity(entityId, userId);
        var item = itemRepo.findById(itemId)
            .orElseThrow(() -> new InventoryException("INVENTORY_ITEM_NOT_FOUND", "Item not found"));
        if (!"CONSUMABLE".equals(item.getType()))
            throw new InventoryException("INVENTORY_NOT_CONSUMABLE", "Item is not consumable");

        var effect = parseEffect(item.getMetadataJson());
        if (effect == null)
            throw new InventoryException("INVENTORY_NO_EFFECT", "Item has no use effect defined");

        int total = 0;
        // Item-Effekte sind systemunabhängig (Heiltrank = 2d4+2 in jedem System)
        // Daher direkte DiceExpression statt RollService (der System-Mods anwenden würde)
        if (effect.heal != null) {
            total = new DiceExpression(effect.heal).getTotal();
        }
        if (effect.damage != null) {
            total = -new DiceExpression(effect.damage).getTotal();
        }

        // Quantity decrement
        var inventory = parseInventory(entity.getInventoryJson());
        var existing = inventory.stream().filter(e -> e.itemId().equals(itemId)).findFirst()
            .orElseThrow(() -> new InventoryException("INVENTORY_ITEM_NOT_FOUND", "Item not in inventory"));

        var newQty = existing.quantity() - 1;
        if (newQty <= 0) {
            inventory.remove(existing);
        } else {
            var idx = inventory.indexOf(existing);
            inventory.set(idx, new RawEntry(existing.itemId(), newQty, existing.equipped(), existing.slot()));
        }
        entity.setInventoryJson(toJson(inventory));
        entityRepo.save(entity);

        return total;
    }

    /** Waffen-Schadensart aus Item-Metadata (P23-T02). */
    private String damageTypeOf(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) return null;
        try {
            var node = objectMapper.readTree(metadataJson).path("damage_type");
            return node.isTextual() && !node.asText().isBlank() ? node.asText() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private EffectSpec parseEffect(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) return null;
        try {
            var tree = objectMapper.readTree(metadataJson);
            var eff = tree.path("effect");
            if (eff.isMissingNode()) return null;
            return new EffectSpec(
                eff.path("heal").asText(null),
                eff.path("damage").asText(null));
        } catch (Exception e) {
            return null;
        }
    }

    record EffectSpec(String heal, String damage) {}

    private GameEntity findEntity(UUID entityId, UUID userId) {
        var entity = entityRepo.findById(entityId)
            .orElseThrow(() -> new InventoryException("ENTITY_NOT_FOUND", "Entity not found"));
        worldAccess.requireAccess(entity.getWorldId(), userId);
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
        // armor_class in attributes_json aktualisieren (immer neu berechnen)
        var baseAc = 10 + bonuses.getOrDefault("armor_class", 0);
        try {
            var tree = objectMapper.readTree(entity.getAttributesJson());
            ((com.fasterxml.jackson.databind.node.ObjectNode) tree).put("armor_class", baseAc);
            entity.setAttributesJson(objectMapper.writeValueAsString(tree));
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
                item.getName(), item.getType(), item.getWeight(), damageTypeOf(item.getMetadataJson())));
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
                                 String name, String type, BigDecimal weight, String damageType) {}

    public record InventoryResult(List<InventoryEntry> items, Map<String, Integer> computedBonuses) {}

    public static class InventoryException extends RuntimeException {
        private final String errorCode;
        public InventoryException(String errorCode, String message) { super(message); this.errorCode = errorCode; }
        public String getErrorCode() { return errorCode; }
    }
}