package com.lwe.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwe.core.domain.GameEntity;
import com.lwe.core.domain.GameSystem;
import com.lwe.core.repository.GameEntityRepository;
import com.lwe.core.repository.GameSystemRepository;
import com.lwe.core.repository.WorldRepository;
import com.lwe.core.util.WorldAccess;
import com.lwe.rules.DiceExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class RestService {

    private final GameEntityRepository entityRepo;
    private final GameSystemRepository gameSystemRepo;
    private final WorldRepository worldRepo;
    private final WorldAccess worldAccess;
    private final ObjectMapper mapper;

    public RestService(GameEntityRepository entityRepo, GameSystemRepository gameSystemRepo,
                       WorldRepository worldRepo, WorldAccess worldAccess,
                        ObjectMapper mapper) {
        this.mapper = mapper;
        this.entityRepo = entityRepo;
        this.gameSystemRepo = gameSystemRepo;
        this.worldRepo = worldRepo;
        this.worldAccess = worldAccess;
    }

    @Transactional
    public void shortRest(UUID entityId, UUID userId) {
        var entity = findEntity(entityId, userId);
        var config = parseRestConfig(entity.getWorldId(), "short_rest");
        if (config == null) return;
        applyHpRecovery(entity, config.hp);
        applyApRecovery(entity, config.ap);
        entityRepo.save(entity);
    }

    @Transactional
    public void longRest(UUID entityId, UUID userId) {
        var entity = findEntity(entityId, userId);
        var config = parseRestConfig(entity.getWorldId(), "long_rest");
        if (config == null) return;
        applyHpRecovery(entity, config.hp);
        applyApRecovery(entity, config.ap);
        entityRepo.save(entity);
    }

    private GameEntity findEntity(UUID entityId, UUID userId) {
        var entity = entityRepo.findById(entityId)
            .orElseThrow(() -> new RestException("ENTITY_NOT_FOUND", "Entity not found"));
        worldAccess.requireAccess(entity.getWorldId(), userId);
        return entity;
    }

    private void applyHpRecovery(GameEntity entity, String hpConfig) {
        if (hpConfig == null || hpConfig.isBlank()) return;
        var hpExpr = hpConfig.strip();
        int healed;
        if ("full".equalsIgnoreCase(hpExpr)) {
            healed = entity.getHpMax() - entity.getHpCurrent();
            entity.setHpCurrent(entity.getHpMax());
        } else if (hpExpr.endsWith("%")) {
            var pct = Integer.parseInt(hpExpr.replace("%", ""));
            var pctHeal = (int) Math.round(entity.getHpMax() * pct / 100.0);
            healed = Math.min(pctHeal, entity.getHpMax() - entity.getHpCurrent());
            entity.setHpCurrent(entity.getHpCurrent() + healed);
        } else if (hpExpr.matches("\\d+")) {
            var flat = Integer.parseInt(hpExpr);
            healed = Math.min(flat, entity.getHpMax() - entity.getHpCurrent());
            entity.setHpCurrent(entity.getHpCurrent() + healed);
        } else {
            try {
                var roll = new DiceExpression(hpExpr);
                healed = Math.min(roll.getTotal(), entity.getHpMax() - entity.getHpCurrent());
                entity.setHpCurrent(entity.getHpCurrent() + healed);
            } catch (IllegalArgumentException e) {
                throw new RestException("INVALID_HP_EXPR", "Invalid HP expression: " + hpExpr);
            }
        }
    }

    private void applyApRecovery(GameEntity entity, String apConfig) {
        if (apConfig == null || apConfig.isBlank()) return;
        if ("full".equalsIgnoreCase(apConfig)) {
            entity.setApCurrent(entity.getApMax());
        } else if ("half".equalsIgnoreCase(apConfig)) {
            var half = (int) Math.round(entity.getApMax() / 2.0);
            entity.setApCurrent(Math.max(half, entity.getApCurrent()));
        }
    }

    private RestConfig parseRestConfig(UUID worldId, String restType) {
        var world = worldRepo.findById(worldId).orElse(null);
        if (world == null || world.getGameSystemId() == null) return null;
        var gs = gameSystemRepo.findById(world.getGameSystemId()).orElse(null);
        if (gs == null) return null;
        try {
            var tree = mapper.readTree(gs.getRulesJson());
            var resting = tree.path("dice_mechanics").path("combat").path("resting").path(restType);
            if (resting.isMissingNode()) return null;
            return new RestConfig(
                resting.path("hp").asText(null),
                resting.path("ap").asText(null));
        } catch (Exception e) {
            return null;
        }
    }

    private record RestConfig(String hp, String ap) {}

    public static class RestException extends RuntimeException {
        private final String errorCode;
        public RestException(String errorCode, String message) { super(message); this.errorCode = errorCode; }
        public String getErrorCode() { return errorCode; }
    }
}
