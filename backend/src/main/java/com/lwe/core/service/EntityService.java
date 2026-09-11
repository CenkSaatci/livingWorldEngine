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
    private final RulesLoader rulesLoader;
    private final ConditionService conditionService;
    private static final TypeReference<Map<String, Integer>> ATTR_MAP = new TypeReference<>() {};

    public EntityService(GameEntityRepository entityRepo, WorldAccess worldAccess,
                        ObjectMapper objectMapper, RulesLoader rulesLoader,
                        ConditionService conditionService) {
        this.objectMapper = objectMapper;
        this.entityRepo = entityRepo;
        this.worldAccess = worldAccess;
        this.rulesLoader = rulesLoader;
        this.conditionService = conditionService;
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
        worldAccess.requireRead(worldId, userId); // T33-02
        if (entityType != null) {
            return entityRepo.findByWorldIdAndEntityTypeAndActiveTrue(worldId, entityType);
        }
        return entityRepo.findByWorldIdAndActiveTrue(worldId);
    }

    public GameEntity getById(UUID entityId, UUID userId) {
        var entity = entityRepo.findById(entityId)
            .orElseThrow(() -> new EntityException("ENTITY_NOT_FOUND", "Entity not found"));
        worldAccess.requireRead(entity.getWorldId(), userId); // T33-02
        return entity;
    }

    @Transactional
    public GameEntity update(UUID entityId, UUID userId, String name, String attributesJson,
                              String inventoryJson, String positionJson, String metadataJson,
                              String backstory, Integer age, String experienceLevel,
                              String socialStanding, UUID factionId) {
        var entity = getById(entityId, userId);
        requireWorldAccess(entity.getWorldId(), userId);
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
        requireWorldAccess(entity.getWorldId(), userId); // F1: Write-Guard
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
        requireWorldAccess(entity.getWorldId(), userId); // F1: Write-Guard
        entity.setExperiencePoints(experiencePoints);
        return entityRepo.save(entity);
    }

    @Transactional
    public GameEntity updateSkills(UUID entityId, UUID userId, Map<String, Integer> skills) {
        return updateSkills(entityId, userId, skills, null);
    }

    /**
     * Skill-Werte setzen. Mit campaignId + advancement.maxRule wird die
     * Max-Regel geprüft (Skill ≤ höchstes beteiligtes Attribut + 2).
     */
    // ponytail: Max gilt für den gespeicherten Skill-Wert (Override), nicht die
    // Sheet-Anzeige inkl. Attributs-Modifikator — Heldenbau präzisiert das später.
    @Transactional
    public GameEntity updateSkills(UUID entityId, UUID userId, Map<String, Integer> skills, UUID campaignId) {
        var entity = getById(entityId, userId);
        requireWorldAccess(entity.getWorldId(), userId);
        enforceSkillMax(entity, skills, campaignId);
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

    @SuppressWarnings("unchecked")
    private void enforceSkillMax(GameEntity entity, Map<String, Integer> skills, UUID campaignId) {
        if (campaignId == null || skills.isEmpty()) return;
        if (!rulesLoader.campaignBelongsToWorld(campaignId, entity.getWorldId())) {
            throw new EntityException("WORLD_ACCESS_DENIED", "Campaign does not belong to world");
        }
        var rules = rulesLoader.loadRules(campaignId, entity.getWorldId());
        var adv = rules.get("advancement");
        if (!(adv instanceof Map<?, ?> advMap)
            || !"highestAttributePlus2".equals(advMap.get("maxRule"))) return;
        var skillDefs = (List<Map<String, Object>>) rules.getOrDefault("skills", List.of());
        var attrs = parseAttrs(entity);
        if (attrs.isEmpty()) {
            // Audit P28: frischer Charakter — Defaults aus rulesJson zaehlen.
            attrs = new java.util.HashMap<>();
            if (rules.get("attributes") instanceof List<?> attrDefs) {
                for (var def : attrDefs) {
                    if (def instanceof Map<?, ?> m && m.get("name") instanceof String n) {
                        var dv = m.get("default");
                        attrs.put(n, dv instanceof Number num ? num.intValue() : 10);
                    }
                }
            }
        }
        for (var e : skills.entrySet()) {
            var def = skillDefs.stream()
                .filter(d -> e.getKey().equals(d.get("name")))
                .findFirst().orElse(null);
            if (def == null) continue;
            // Legacy "attribute" (Singular) mitlesen (Audit P28).
            List<String> involved = def.get("attributes") instanceof List<?> l
                ? (List<String>) l
                : (def.get("attribute") instanceof String a ? List.of(a) : List.of());
            var max = involved.stream()
                .map(attrs::get).filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue).max().orElse(-1);
            if (max >= 0 && e.getValue() > max + 2) {
                throw new EntityException("SKILL_MAX_EXCEEDED",
                    "Skill " + e.getKey() + " exceeds max " + (max + 2));
            }
        }
    }

    private Map<String, Integer> parseAttrs(GameEntity entity) {
        try {
            var raw = entity.getAttributesJson();
            if (raw == null || raw.isBlank()) return Map.of();
            return objectMapper.readValue(raw, ATTR_MAP);
        } catch (Exception e) {
            return Map.of();
        }
    }

    /** Entity mit Schreib-Lock laden (verhindert Metadata-Races bei fate/conditions). */
    private GameEntity getLocked(UUID entityId, UUID userId) {
        var entity = entityRepo.findByIdForUpdate(entityId)
            .orElseThrow(() -> new EntityException("ENTITY_NOT_FOUND", "Entity not found"));
        worldAccess.requireAccess(entity.getWorldId(), userId);
        return entity;
    }

    /** Schicksalspunkt ausgeben (P29-T02). */
    @Transactional
    public GameEntity spendFatePoint(UUID entityId, UUID userId, UUID campaignId) {
        var entity = getLocked(entityId, userId);
        int max = 0;
        if (campaignId != null && rulesLoader.campaignBelongsToWorld(campaignId, entity.getWorldId())) {
            var rules = rulesLoader.loadRules(campaignId, entity.getWorldId());
            if (rules.get("creationBudget") instanceof Map<?, ?> b && b.get("fatePoints") instanceof Number n) {
                max = n.intValue();
            }
        }
        int current = fatePoints(entity, max);
        if (current <= 0) throw new EntityException("FATE_NONE_LEFT", "No fate points left");
        writeFatePoints(entity, current - 1);
        return entityRepo.save(entity);
    }

    /** Aktuelle Schicksalspunkte (metadataJson.fate_points), Default = Budget-Wert. */
    public int fatePoints(GameEntity entity, int defaultMax) {
        if (entity.getMetadataJson() == null || entity.getMetadataJson().isBlank()) return defaultMax;
        try {
            var node = objectMapper.readTree(entity.getMetadataJson()).path("fate_points");
            return node.isInt() || node.isLong() ? node.asInt() : defaultMax;
        } catch (Exception e) {
            return defaultMax;
        }
    }

    private void writeFatePoints(GameEntity entity, int value) {
        try {
            ObjectNode meta;
            if (entity.getMetadataJson() == null || entity.getMetadataJson().isBlank()) {
                meta = objectMapper.createObjectNode();
            } else {
                var parsed = objectMapper.readTree(entity.getMetadataJson());
                meta = parsed.isObject() ? (ObjectNode) parsed : objectMapper.createObjectNode();
            }
            meta.put("fate_points", value);
            entity.setMetadataJson(objectMapper.writeValueAsString(meta));
        } catch (Exception e) {
            throw new RuntimeException("Failed to update fate points", e);
        }
    }

    /** Zustand anwenden (P29-T01); Katalog prueft/freigibt die Namen. */
    @Transactional
    public GameEntity addCondition(UUID entityId, UUID userId, String name, Integer rounds, UUID campaignId) {
        var entity = getLocked(entityId, userId);
        worldAccess.requireDm(entity.getWorldId(), userId);
        if (campaignId != null) {
            if (!rulesLoader.campaignBelongsToWorld(campaignId, entity.getWorldId())) {
                throw new EntityException("WORLD_ACCESS_DENIED", "Campaign does not belong to world");
            }
            var rules = rulesLoader.loadRules(campaignId, entity.getWorldId());
            if (rules.get("conditions") instanceof List<?> catalog && !catalog.isEmpty()) {
                var match = catalog.stream()
                    .filter(c -> c instanceof Map<?, ?> m && name.equals(m.get("name")))
                    .findFirst();
                if (match.isEmpty())
                    throw new EntityException("UNKNOWN_CONDITION", "Unknown condition: " + name);
                // Katalog-Dauer greift, wenn kein explizites rounds mitgegeben wurde.
                if (rounds == null && match.get() instanceof Map<?, ?> m
                    && m.get("rounds") instanceof Number r) {
                    rounds = r.intValue();
                }
            }
        }
        conditionService.add(entity, new ConditionService.ConditionInstance(name, rounds));
        return entityRepo.save(entity);
    }

    @Transactional
    public GameEntity removeCondition(UUID entityId, UUID userId, String name) {
        var entity = getLocked(entityId, userId);
        worldAccess.requireDm(entity.getWorldId(), userId);
        conditionService.remove(entity, name);
        return entityRepo.save(entity);
    }

    @Transactional
    public GameEntity updateOverrides(UUID entityId, UUID userId, Map<String, Object> overrides) {
        var entity = getById(entityId, userId);
        requireWorldAccess(entity.getWorldId(), userId); // F1: Write-Guard
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
        requireWorldAccess(entity.getWorldId(), userId);
        entity.setActive(false);
        entityRepo.save(entity);
    }

    /** N2-Audit: Schreibzugriff auf eine Entity (Read-Guard + Write-Guard). */
    public void requireWriteAccess(UUID entityId, UUID userId) {
        var entity = getById(entityId, userId);
        requireWorldAccess(entity.getWorldId(), userId);
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