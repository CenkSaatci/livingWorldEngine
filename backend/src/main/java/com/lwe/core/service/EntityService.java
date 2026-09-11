package com.lwe.core.service;

import com.lwe.core.domain.GameEntity;
import com.lwe.core.repository.GameEntityRepository;
import com.lwe.core.repository.WorldRepository;
import com.lwe.core.util.WorldAccess;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class EntityService {

    private final GameEntityRepository entityRepo;
    private final WorldAccess worldAccess;
    private final ObjectMapper objectMapper;
    private static final TypeReference<Map<String, Integer>> ATTR_MAP = new TypeReference<>() {};

    public EntityService(GameEntityRepository entityRepo, WorldAccess worldAccess,
                        ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.entityRepo = entityRepo;
        this.worldAccess = worldAccess;
    }

    @Transactional
    public GameEntity create(UUID worldId, UUID userId, String entityType, String name,
                              String attributesJson, String inventoryJson, String positionJson,
                              String metadataJson, UUID factionId,
                              String backstory, Integer age, String experienceLevel,
                              String socialStanding) {
        requireWorldAccess(worldId, userId);

        var entity = new GameEntity(worldId, entityType, name);
        if (nonBlank(attributesJson)) entity.setAttributesJson(attributesJson);
        if (nonBlank(inventoryJson)) entity.setInventoryJson(inventoryJson);
        if (nonBlank(positionJson)) entity.setPositionJson(positionJson);
        if (nonBlank(metadataJson)) entity.setMetadataJson(metadataJson);
        if (factionId != null) entity.setFactionId(factionId);
        if (backstory != null) entity.setBackstory(backstory);
        if (age != null) entity.setAge(age);
        if (experienceLevel != null) entity.setExperienceLevel(experienceLevel);
        if (socialStanding != null) entity.setSocialStanding(socialStanding);

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
                              String inventoryJson, String positionJson, String metadataJson,
                              String backstory, Integer age, String experienceLevel,
                              String socialStanding, UUID factionId) {
        var entity = getById(entityId, userId);
        if (name != null) entity.setName(name);
        if (nonBlank(attributesJson)) entity.setAttributesJson(attributesJson);
        if (nonBlank(inventoryJson)) entity.setInventoryJson(inventoryJson);
        if (nonBlank(positionJson)) entity.setPositionJson(positionJson);
        if (nonBlank(metadataJson)) entity.setMetadataJson(metadataJson);
        if (backstory != null) entity.setBackstory(backstory);
        if (age != null) entity.setAge(age);
        if (experienceLevel != null) entity.setExperienceLevel(experienceLevel);
        if (socialStanding != null) entity.setSocialStanding(socialStanding);
        if (factionId != null) entity.setFactionId(factionId);
        return entityRepo.save(entity);
    }

    @Transactional
    public GameEntity updateAttributes(UUID entityId, UUID userId, Map<String, Integer> newAttrs) {
        var entity = getById(entityId, userId);
        try {
            var raw = entity.getAttributesJson();
            if (raw == null || raw.isBlank()) raw = "{}";
            var current = objectMapper.readValue(raw, ATTR_MAP);
            current.putAll(newAttrs);
            entity.setAttributesJson(objectMapper.writeValueAsString(current));
            return entityRepo.save(entity);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update attributes", e);
        }
    }

    @Transactional
    public GameEntity updateProgression(UUID entityId, UUID userId, int experiencePoints, Integer level) {
        var entity = getById(entityId, userId);
        entity.setExperiencePoints(experiencePoints);
        return entityRepo.save(entity);
    }

    @Transactional
    public GameEntity updateSkills(UUID entityId, UUID userId, Map<String, Integer> skills) {
        var entity = getById(entityId, userId);
        try {
            var existing = entity.getSkillsJson() != null && !entity.getSkillsJson().isBlank()
                ? objectMapper.readValue(entity.getSkillsJson(), ATTR_MAP)
                : new java.util.HashMap<String, Integer>();
            existing.putAll(skills);
            entity.setSkillsJson(objectMapper.writeValueAsString(existing));
            return entityRepo.save(entity);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update skills", e);
        }
    }

    @Transactional
    public GameEntity updateOverrides(UUID entityId, UUID userId, Map<String, Object> overrides) {
        var entity = getById(entityId, userId);
        try {
            var meta = entity.getMetadataJson() != null
                ? objectMapper.readTree(entity.getMetadataJson())
                : objectMapper.createObjectNode();
            ((ObjectNode) meta).set("formula_overrides", objectMapper.valueToTree(overrides));
            entity.setMetadataJson(objectMapper.writeValueAsString(meta));
            return entityRepo.save(entity);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update overrides", e);
        }
    }

    @Transactional
    public void delete(UUID entityId, UUID userId) {
        var entity = getById(entityId, userId);
        entity.setActive(false);
        entityRepo.save(entity);
    }

    private void requireWorldAccess(UUID worldId, UUID userId) {
        worldAccess.requireAccess(worldId, userId);
    }

    private static boolean nonBlank(String v) {
        return v != null && !v.isBlank();
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