package com.lwe.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lwe.core.domain.GameEntity;
import com.lwe.core.domain.GameSystem;
import com.lwe.core.repository.GameEntityRepository;
import com.lwe.core.repository.GameSystemRepository;
import com.lwe.core.repository.WorldRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class LevelUpService {

    private final GameEntityRepository entityRepo;
    private final GameSystemRepository gameSystemRepo;
    private final WorldRepository worldRepo;
    private final ObjectMapper mapper = new ObjectMapper();

    public LevelUpService(GameEntityRepository entityRepo,
                          GameSystemRepository gameSystemRepo,
                          WorldRepository worldRepo) {
        this.entityRepo = entityRepo;
        this.gameSystemRepo = gameSystemRepo;
        this.worldRepo = worldRepo;
    }

    @Transactional
    public void addXp(UUID entityId, int amount) {
        var entity = entityRepo.findById(entityId)
            .orElseThrow(() -> new LevelException("ENTITY_NOT_FOUND", "Entity not found"));
        var gameSystem = resolveGameSystem(entity.getWorldId());

        int oldLevel = getLevel(entity, gameSystem);
        entity.setExperiencePoints(entity.getExperiencePoints() + amount);
        int newLevel = getLevel(entity, gameSystem);

        if (newLevel > oldLevel) {
            int gained = sumPointsForLevels(gameSystem, oldLevel + 1, newLevel);
            entity.setUnspentAttributePoints(entity.getUnspentAttributePoints() + gained);
        }

        entityRepo.save(entity);
    }

    public int getLevel(GameEntity entity, GameSystem gameSystem) {
        var progression = parseProgression(gameSystem.getRulesJson());
        if (progression.levels == null || progression.levels.isEmpty()) return 1;

        int level = 1;
        for (var l : progression.levels) {
            if (entity.getExperiencePoints() >= l.xp) level = l.level;
        }
        return level;
    }

    @Transactional
    public void spendPoints(UUID entityId, String attribute, int points) {
        var entity = entityRepo.findById(entityId)
            .orElseThrow(() -> new LevelException("ENTITY_NOT_FOUND", "Entity not found"));

        if (entity.getUnspentAttributePoints() < points)
            throw new LevelException("INSUFFICIENT_POINTS", "Not enough unspent attribute points");

        try {
            var tree = mapper.readTree(entity.getAttributesJson());
            var node = tree.path(attribute);
            int current = node.isInt() ? node.asInt() : 10;
            ((ObjectNode) tree).put(attribute, current + points);

            var spent = tree.path("__spent__");
            var spentObj = spent.isObject() ? (ObjectNode) spent : ((ObjectNode) tree).putObject("__spent__");
            spentObj.put(attribute, spentObj.path(attribute).asInt(0) + points);

            entity.setAttributesJson(mapper.writeValueAsString(tree));
        } catch (Exception e) {
            throw new LevelException("ATTRIBUTE_PARSE_ERROR", "Failed to parse attributes");
        }

        entity.setUnspentAttributePoints(entity.getUnspentAttributePoints() - points);
        entityRepo.save(entity);
    }

    @Transactional
    public void resetPoints(UUID entityId) {
        var entity = entityRepo.findById(entityId)
            .orElseThrow(() -> new LevelException("ENTITY_NOT_FOUND", "Entity not found"));

        try {
            var tree = mapper.readTree(entity.getAttributesJson());
            var spent = tree.path("__spent__");
            if (spent.isObject()) {
                int totalSpent = 0;
                var it = spent.fields();
                while (it.hasNext()) {
                    var entry = it.next();
                    int pts = entry.getValue().asInt(0);
                    totalSpent += pts;
                    var attr = tree.path(entry.getKey());
                    if (attr.isInt()) {
                        ((ObjectNode) tree).put(entry.getKey(), attr.asInt() - pts);
                    }
                }
                ((ObjectNode) tree).remove("__spent__");
                entity.setAttributesJson(mapper.writeValueAsString(tree));
                entity.setUnspentAttributePoints(entity.getUnspentAttributePoints() + totalSpent);
            }
        } catch (Exception e) {
            throw new LevelException("ATTRIBUTE_PARSE_ERROR", "Failed to parse attributes");
        }
        entityRepo.save(entity);
    }

    private GameSystem resolveGameSystem(UUID worldId) {
        var world = worldRepo.findById(worldId).orElse(null);
        if (world == null || world.getGameSystemId() == null) return null;
        return gameSystemRepo.findById(world.getGameSystemId()).orElse(null);
    }

    private ProgressionConfig parseProgression(String rulesJson) {
        if (rulesJson == null) return new ProgressionConfig("level", null, null);
        try {
            var tree = mapper.readTree(rulesJson);
            var prog = tree.path("progression");
            if (prog.isMissingNode()) return new ProgressionConfig("level", null, null);

            var mode = prog.path("mode").asText("level");
            var levels = prog.path("levels");
            java.util.List<LevelDef> levelList = null;
            if (levels.isArray()) {
                var list = new java.util.ArrayList<LevelDef>();
                for (var l : levels) {
                    list.add(new LevelDef(
                        l.path("level").asInt(1),
                        l.path("xp").asInt(0),
                        l.path("attribute_points").asInt(0),
                        l.path("ability_slots").asInt(0)
                    ));
                }
                levelList = list;
            }
            return new ProgressionConfig(mode, levelList, null);
        } catch (Exception e) {
            return new ProgressionConfig("level", null, null);
        }
    }

    private int sumPointsForLevels(GameSystem gs, int from, int to) {
        var prog = parseProgression(gs.getRulesJson());
        if (prog.levels == null) return 0;
        int sum = 0;
        for (var l : prog.levels) {
            if (l.level >= from && l.level <= to) sum += l.attributePoints;
        }
        return sum;
    }

    record ProgressionConfig(String mode, java.util.List<LevelDef> levels, Object shopPrices) {}
    record LevelDef(int level, int xp, int attributePoints, int abilitySlots) {}

    public static class LevelException extends RuntimeException {
        private final String errorCode;
        public LevelException(String errorCode, String message) { super(message); this.errorCode = errorCode; }
        public String getErrorCode() { return errorCode; }
    }
}
