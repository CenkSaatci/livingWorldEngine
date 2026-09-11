package com.lwe.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.lwe.core.domain.GameItem;
import com.lwe.core.domain.GameSystem;
import com.lwe.core.repository.GameEntityRepository;
import com.lwe.core.repository.GameItemRepository;
import com.lwe.core.repository.GameSystemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ItemService {

    private static final Set<String> VALID_TYPES = Set.of("WEAPON", "ARMOR", "CONSUMABLE", "MISC");

    private final GameItemRepository itemRepo;
    private final GameSystemRepository systemRepo;
    private final GameEntityRepository entityRepo;
    private final ObjectMapper objectMapper;

    public ItemService(GameItemRepository itemRepo, GameSystemRepository systemRepo,
                       GameEntityRepository entityRepo, ObjectMapper objectMapper) {
        this.itemRepo = itemRepo;
        this.systemRepo = systemRepo;
        this.entityRepo = entityRepo;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public GameItem create(UUID gameSystemId, UUID userId, String name, String type,
                           BigDecimal weight, int value, String bonusesJson, String metadataJson) {
        requireSystem(gameSystemId);
        if (!VALID_TYPES.contains(type)) {
            throw new ItemException("INVALID_ITEM_TYPE", "Type must be one of " + VALID_TYPES);
        }
        var item = new GameItem(gameSystemId, name, type,
            weight != null ? weight : BigDecimal.ZERO, value);
        if (bonusesJson != null) item.setBonusesJson(bonusesJson);
        if (metadataJson != null) item.setMetadataJson(metadataJson);
        return itemRepo.save(item);
    }

    public List<GameItem> listByGameSystem(UUID gameSystemId) {
        requireSystem(gameSystemId);
        return itemRepo.findByGameSystemId(gameSystemId);
    }

    public GameItem getById(UUID id) {
        return itemRepo.findById(id)
            .orElseThrow(() -> new ItemException("ITEM_NOT_FOUND", "Item not found"));
    }

    @Transactional
    public GameItem update(UUID id, String name, String type, BigDecimal weight,
                           Integer value, String bonusesJson, String metadataJson) {
        var item = getById(id);
        if (name != null) item.setName(name);
        if (bonusesJson != null) item.setBonusesJson(bonusesJson);
        if (metadataJson != null) item.setMetadataJson(metadataJson);
        return itemRepo.save(item);
    }

    @Transactional
    public void delete(UUID id) {
        var item = getById(id);
        // BUG-9: equipped item löschen würde Inventare brick-en
        // (GET → 404 INVENTORY_ITEM_NOT_FOUND). Erst überall ent-equippen/entfernen.
        for (var entity : entityRepo.findAll()) {
            var inv = entity.getInventoryJson();
            if (inv == null || inv.isBlank()) continue;
            try {
                var arr = (ArrayNode) objectMapper.readTree(inv);
                var before = arr.size();
                for (int i = arr.size() - 1; i >= 0; i--) {
                    if (id.toString().equals(arr.get(i).path("itemId").asText(null))) arr.remove(i);
                }
                if (arr.size() != before) {
                    entity.setInventoryJson(objectMapper.writeValueAsString(arr));
                    entityRepo.save(entity);
                }
            } catch (Exception ignored) {}
        }
        itemRepo.delete(item);
    }

    private void requireSystem(UUID gameSystemId) {
        systemRepo.findById(gameSystemId)
            .filter(GameSystem::isActive)
            .orElseThrow(() -> new ItemException("GAME_SYSTEM_NOT_FOUND", "Game system not found or inactive"));
    }

    public static class ItemException extends RuntimeException {
        private final String errorCode;
        public ItemException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }
        public String getErrorCode() { return errorCode; }
    }
}
