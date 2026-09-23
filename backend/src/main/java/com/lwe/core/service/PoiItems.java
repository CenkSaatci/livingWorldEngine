package com.lwe.core.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwe.core.domain.GameEntity;
import com.lwe.core.domain.GameItem;
import com.lwe.core.domain.GameSystem;
import com.lwe.core.repository.GameItemRepository;
import com.lwe.core.util.RuleNames;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ADR-015: Item-Zugriffe für POI-Effekte und Handel. Bündelt Namensauflösung
 * (case-insensitiv), Bestandsprüfung und Inventar-Änderungen an einer Stelle.
 */
@Component
public class PoiItems {

    private static final TypeReference<List<Map<String, Object>>> ITEM_LIST = new TypeReference<>() {};

    private final GameItemRepository itemRepo;
    private final InventoryService inventoryService;
    private final ObjectMapper mapper;

    public PoiItems(GameItemRepository itemRepo, InventoryService inventoryService,
                    ObjectMapper mapper) {
        this.itemRepo = itemRepo;
        this.inventoryService = inventoryService;
        this.mapper = mapper;
    }

    /** Item des Systems per Name (case-insensitiv); unbekannt = Fehler. */
    public GameItem resolve(GameSystem system, String name) {
        if (name == null) throw new PoiException("POI_ITEM_UNKNOWN", "Item name missing");
        if (system == null) throw new PoiException("POI_ITEM_UNKNOWN", "No game system for items");
        return itemRepo.findByGameSystemId(system.getId()).stream()
            .filter(i -> RuleNames.eq(i.getName(), name))
            .findFirst()
            .orElseThrow(() -> new PoiException("POI_ITEM_UNKNOWN", "Unknown item: " + name));
    }

    /** Bestand eines Items beim Charakter (0 bei unlesbarem Inventar). */
    public int quantity(GameEntity owner, UUID itemId) {
        try {
            var raw = owner.getInventoryJson();
            if (raw == null || raw.isBlank()) return 0;
            List<Map<String, Object>> inv = mapper.readValue(raw, ITEM_LIST);
            return inv.stream()
                .filter(e -> itemId.toString().equals(String.valueOf(e.get("itemId"))))
                .mapToInt(e -> e.get("quantity") instanceof Number n ? n.intValue() : 0)
                .sum();
        } catch (Exception e) {
            return 0;
        }
    }

    public void add(GameEntity actor, UUID itemId, int qty) {
        inventoryService.addItemInternal(actor, itemId, qty);
    }

    public void remove(GameEntity actor, UUID itemId, int qty) {
        inventoryService.removeItemInternal(actor, itemId, qty);
    }
}
