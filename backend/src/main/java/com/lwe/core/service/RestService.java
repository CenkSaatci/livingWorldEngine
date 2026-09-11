package com.lwe.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwe.core.domain.GameEntity;
import com.lwe.core.domain.GameSystem;
import com.lwe.core.repository.GameEntityRepository;
import com.lwe.core.util.WorldAccess;
import com.lwe.rules.DiceExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class RestService {

    private final GameEntityRepository entityRepo;
    private final WorldAccess worldAccess;
    private final RulesLoader rulesLoader;
    private final ObjectMapper mapper;

    public RestService(GameEntityRepository entityRepo, WorldAccess worldAccess,
                       RulesLoader rulesLoader, ObjectMapper mapper) {
        this.mapper = mapper;
        this.entityRepo = entityRepo;
        this.worldAccess = worldAccess;
        this.rulesLoader = rulesLoader;
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
        if (entity.getHpMax() <= 0) return;
        var hpExpr = hpConfig.strip();
        int healed;
        if ("full".equalsIgnoreCase(hpExpr)) {
            healed = Math.max(0, entity.getHpMax() - entity.getHpCurrent());
            entity.setHpCurrent(entity.getHpMax());
        } else if (hpExpr.endsWith("%")) {
            var pct = Integer.parseInt(hpExpr.replace("%", ""));
            var pctHeal = (int) Math.round(entity.getHpMax() * pct / 100.0);
            healed = Math.max(0, Math.min(pctHeal, entity.getHpMax() - entity.getHpCurrent()));
            entity.setHpCurrent(entity.getHpCurrent() + healed);
        } else if (hpExpr.matches("\\d+")) {
            var flat = Integer.parseInt(hpExpr);
            healed = Math.max(0, Math.min(flat, entity.getHpMax() - entity.getHpCurrent()));
            entity.setHpCurrent(entity.getHpCurrent() + healed);
        } else {
            try {
                var roll = new DiceExpression(hpExpr);
                healed = Math.max(0, Math.min(roll.getTotal(), entity.getHpMax() - entity.getHpCurrent()));
                entity.setHpCurrent(entity.getHpCurrent() + healed);
            } catch (IllegalArgumentException e) {
                throw new RestException("INVALID_HP_EXPR", "Invalid HP expression: " + hpExpr);
            }
        }
    }

    private void applyApRecovery(GameEntity entity, String apConfig) {
        if (apConfig == null || apConfig.isBlank()) return;
        if (entity.getApMax() <= 0) return;
        if ("full".equalsIgnoreCase(apConfig)) {
            entity.setApCurrent(entity.getApMax());
        } else if ("half".equalsIgnoreCase(apConfig)) {
            var half = (int) Math.round(entity.getApMax() / 2.0);
            entity.setApCurrent(Math.max(half, entity.getApCurrent()));
        }
    }

    private RestConfig parseRestConfig(UUID worldId, String restType) {
        var gs = rulesLoader.loadSystem(worldId);
        if (gs == null) return null;
        try {
            var tree = mapper.readTree(gs.getRulesJson());
            var resting = tree.path("dice_mechanics").path("combat").path("resting").path(restType);
            if (resting.isMissingNode() || resting.isNull()) return null;
            // Legacy keys: hp/ap (e.g. "50%", "full", dice expr). Wizard/dnd5e keys:
            // short_rest {heal_percent, recover_resources}, long_rest {full_heal, recover_all}.
            // Accept snake_case and camelCase variants.
            String hp = textOrNull(resting, "hp");
            if (hp == null) {
                if (isTrue(resting, "full_heal", "fullHeal")) {
                    hp = "full";
                } else {
                    Double pct = numberOrNull(resting, "heal_percent", "healPercent");
                    if (pct != null) hp = toPercentString(pct);
                }
            }
            String ap = textOrNull(resting, "ap");
            if (ap == null
                && isTrue(resting, "recover_all", "recoverAll",
                    "recover_resources", "recoverResources")) {
                ap = "full";
            }
            return new RestConfig(hp, ap);
        } catch (Exception e) {
            return null;
        }
    }

    private static String textOrNull(com.fasterxml.jackson.databind.JsonNode node, String field) {
        var child = node.path(field);
        if (child.isMissingNode() || child.isNull()) return null;
        var text = child.asText(null);
        return (text == null || text.isBlank() || "null".equals(text)) ? null : text;
    }

    private static boolean isTrue(com.fasterxml.jackson.databind.JsonNode node, String... fields) {
        for (var f : fields) {
            var child = node.path(f);
            if (!child.isMissingNode() && !child.isNull() && child.asBoolean(false)) return true;
        }
        return false;
    }

    private static Double numberOrNull(com.fasterxml.jackson.databind.JsonNode node, String... fields) {
        for (var f : fields) {
            var child = node.path(f);
            if (child.isMissingNode() || child.isNull()) continue;
            if (child.isNumber()) return child.asDouble();
            if (child.isTextual()) {
                try {
                    return Double.parseDouble(child.asText().strip().replace("%", ""));
                } catch (NumberFormatException ignored) {
                    // fall through
                }
            }
        }
        return null;
    }

    private static String toPercentString(double pct) {
        // Wizard writes a 0..1 fraction (0.5 = 50%); values > 1 are already percentages.
        double percent = pct <= 1.0 ? pct * 100.0 : pct;
        return Math.max(0, (int) Math.round(percent)) + "%";
    }

    private record RestConfig(String hp, String ap) {}

    public static class RestException extends RuntimeException {
        private final String errorCode;
        public RestException(String errorCode, String message) { super(message); this.errorCode = errorCode; }
        public String getErrorCode() { return errorCode; }
    }
}
