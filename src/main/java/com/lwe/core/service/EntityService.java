package com.lwe.core.service;

import com.lwe.core.domain.GameEntity;
import com.lwe.core.repository.GameEntityRepository;
import com.lwe.core.repository.WorldRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class EntityService {

    private final GameEntityRepository entityRepo;
    private final WorldRepository worldRepo;

    public EntityService(GameEntityRepository entityRepo, WorldRepository worldRepo) {
        this.entityRepo = entityRepo;
        this.worldRepo = worldRepo;
    }

    @Transactional
    public GameEntity create(UUID worldId, UUID userId, String entityType, String name,
                             String attributesJson, String inventoryJson, String positionJson,
                             String metadataJson, UUID factionId) {
        requireWorldAccess(worldId, userId);

        var entity = new GameEntity(worldId, entityType, name);
        if (attributesJson != null) entity.setAttributesJson(attributesJson);
        if (inventoryJson != null) entity.setInventoryJson(inventoryJson);
        if (positionJson != null) entity.setPositionJson(positionJson);
        if (metadataJson != null) entity.setMetadataJson(metadataJson);
        if (factionId != null) entity.setFactionId(factionId);

        return entityRepo.save(entity);
    }

    public List<GameEntity> list(UUID worldId, UUID userId, String entityType) {
        requireWorldAccess(worldId, userId);
        if (entityType != null) {
            return entityRepo.findByWorldIdAndEntityTypeAndActiveTrue(worldId, entityType);
        }
        return entityRepo.findByWorldIdAndActiveTrue(worldId);
    }

    public GameEntity getById(UUID entityId, UUID userId) {
        var entity = entityRepo.findById(entityId)
            .orElseThrow(() -> new EntityException("ENTITY_NOT_FOUND", "Entity not found"));
        requireWorldAccess(entity.getWorldId(), userId);
        return entity;
    }

    @Transactional
    public GameEntity update(UUID entityId, UUID userId, String name, String attributesJson,
                             String inventoryJson, String positionJson, String metadataJson) {
        var entity = getById(entityId, userId);
        if (name != null) entity.setName(name);
        if (attributesJson != null) entity.setAttributesJson(attributesJson);
        if (inventoryJson != null) entity.setInventoryJson(inventoryJson);
        if (positionJson != null) entity.setPositionJson(positionJson);
        if (metadataJson != null) entity.setMetadataJson(metadataJson);
        return entityRepo.save(entity);
    }

    @Transactional
    public void delete(UUID entityId, UUID userId) {
        var entity = getById(entityId, userId);
        entity.setActive(false);
        entityRepo.save(entity);
    }

    private void requireWorldAccess(UUID worldId, UUID userId) {
        worldRepo.findById(worldId).ifPresentOrElse(
            world -> {
                if (!world.getOwnerId().equals(userId)) {
                    throw new EntityException("WORLD_ACCESS_DENIED", "Access denied to this world");
                }
            },
            () -> { throw new EntityException("WORLD_NOT_FOUND", "World not found"); }
        );
    }

    public static class EntityException extends RuntimeException {
        private final String errorCode;
        public EntityException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }
        public String getErrorCode() { return errorCode; }
    }
}