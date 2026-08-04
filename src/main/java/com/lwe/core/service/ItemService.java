package com.lwe.core.service;

import com.lwe.core.domain.GameItem;
import com.lwe.core.domain.GameSystem;
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

    public ItemService(GameItemRepository itemRepo, GameSystemRepository systemRepo) {
        this.itemRepo = itemRepo;
        this.systemRepo = systemRepo;
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
