package com.lwe.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwe.core.domain.GameEntity;
import com.lwe.core.domain.GameSystem;
import com.lwe.core.repository.GameEntityRepository;
import com.lwe.core.util.WorldAccess;
import com.lwe.rules.DiceExpression;
import com.lwe.core.util.EntityAccess;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RestService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RestService.class);

    private final GameEntityRepository entityRepo;
    private final WorldAccess worldAccess;
    private final EntityAccess entityAccess;
    private final RulesLoader rulesLoader;
    private final ObjectMapper mapper;
    private final DerivedValueService derivedValueService;

    public RestService(GameEntityRepository entityRepo, WorldAccess worldAccess,
                       EntityAccess entityAccess, RulesLoader rulesLoader, ObjectMapper mapper,
                       DerivedValueService derivedValueService) {
        this.mapper = mapper;
        this.entityRepo = entityRepo;
        this.worldAccess = worldAccess;
        this.entityAccess = entityAccess;
        this.rulesLoader = rulesLoader;
        this.derivedValueService = derivedValueService;
    }

    @Transactional
    public void shortRest(UUID entityId, UUID userId) {
        shortRest(entityId, userId, null);
    }

    @Transactional
    public void shortRest(UUID entityId, UUID userId, UUID campaignId) {
        var entity = findEntity(entityId, userId);
        var config = parseRestConfig(campaignId, entity.getWorldId(), "short_rest");
        if (config == null) return;
        applyHpRecovery(entity, config.hp);
        applyApRecovery(entity, config.ap);
        if (config.recoverResources) restoreCastResources(entity, campaignId, "short");
        entityRepo.save(entity);
    }

    @Transactional
    public void longRest(UUID entityId, UUID userId) {
        longRest(entityId, userId, null);
    }

    @Transactional
    public void longRest(UUID entityId, UUID userId, UUID campaignId) {
        var entity = findEntity(entityId, userId);
        var config = parseRestConfig(campaignId, entity.getWorldId(), "long_rest");
        if (config == null) return;
        applyHpRecovery(entity, config.hp);
        applyApRecovery(entity, config.ap);
        if (config.recoverResources) restoreCastResources(entity, campaignId, "long");
        entityRepo.save(entity);
    }

    /**
     * P1: Casting-Ressourcen generisch auffuellen — jede `skills[].casting.resource`
     * (asp/kap/mp/slot_1/…) mit passendem `restore` (Default "long") wird auf den
     * abgeleiteten Maximalwert gesetzt.
     */
    @SuppressWarnings("unchecked")
    private void restoreCastResources(GameEntity entity, UUID campaignId, String restType) {
        var gs = rulesLoader.loadSystemByCampaign(campaignId);
        if (gs == null) gs = rulesLoader.loadSystem(entity.getWorldId());
        if (gs == null) return;
        try {
            var rules = mapper.readValue(gs.getRulesJson(),
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            var skills = (List<Map<String, Object>>) rules.getOrDefault("skills", List.of());
            var attrs = parseAttrs(entity);
            var traits = selectedTraits(entity);
            var derived = (List<Map<String, Object>>) rules.getOrDefault("derived_values", List.of());
            for (var skill : skills) {
                if (!(skill.get("casting") instanceof Map<?, ?> casting)) continue;
                if (!(casting.get("resource") instanceof String resource)) continue;
                var restore = casting.get("restore") instanceof String r ? r : "long";
                if (!restore.equals(restType)) continue;
                var max = derivedValueService.evaluate(derived, attrs, traits).stream()
                    .filter(dv -> dv.name().equals(resource))
                    .filter(dv -> dv.error() == null) // Audit P1: kaputte Formel => nicht ueberschreiben
                    .map(dv -> (int) Math.round(dv.value()))
                    .findFirst().orElse(null);
                if (max == null) continue;
                writeMetaCounter(entity, resource + "_current", max);
            }
        } catch (Exception e) {
            // Rest darf nie an Regel-Metadaten scheitern — aber sichtbar loggen.
            log.warn("Cast-Resource-Restore fehlgeschlagen: {}", e.getMessage(), e);
        }
    }

    private void writeMetaCounter(GameEntity entity, String key, int value) {
        try {
            var meta = entity.getMetadataJson() == null || entity.getMetadataJson().isBlank()
                ? mapper.createObjectNode()
                : mapper.readTree(entity.getMetadataJson());
            var obj = meta.isObject() ? (com.fasterxml.jackson.databind.node.ObjectNode) meta
                : mapper.createObjectNode();
            obj.put(key, value);
            entity.setMetadataJson(mapper.writeValueAsString(obj));
        } catch (Exception ignored) {
            // ignore
        }
    }

    private Map<String, Integer> parseAttrs(GameEntity entity) {
        try {
            if (entity.getAttributesJson() == null || entity.getAttributesJson().isBlank()) return Map.of();
            return mapper.readValue(entity.getAttributesJson(),
                new com.fasterxml.jackson.core.type.TypeReference<>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private List<String> selectedTraits(GameEntity entity) {
        try {
            if (entity.getMetadataJson() == null || entity.getMetadataJson().isBlank()) return List.of();
            var node = mapper.readTree(entity.getMetadataJson()).path("traits");
            if (!node.isArray()) return List.of();
            var out = new java.util.ArrayList<String>();
            node.forEach(n -> { if (n.isTextual()) out.add(n.asText()); });
            return out;
        } catch (Exception e) {
            return List.of();
        }
    }

    private GameEntity findEntity(UUID entityId, UUID userId) {
        var entity = entityRepo.findById(entityId)
            .orElseThrow(() -> new RestException("ENTITY_NOT_FOUND", "Entity not found"));
        entityAccess.checkControl(entity, userId); // Runde 1: Rasten ist Charakter-Handlung
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

    private RestConfig parseRestConfig(UUID campaignId, UUID worldId, String restType) {
        var gs = rulesLoader.loadSystemByCampaign(campaignId);
        if (gs == null) gs = rulesLoader.loadSystem(worldId);
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
            boolean recoverResources = isTrue(resting, "recover_all", "recoverAll",
                "recover_resources", "recoverResources");
            return new RestConfig(hp, ap, recoverResources);
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

    private record RestConfig(String hp, String ap, boolean recoverResources) {}

    public static class RestException extends RuntimeException {
        private final String errorCode;
        public RestException(String errorCode, String message) { super(message); this.errorCode = errorCode; }
        public String getErrorCode() { return errorCode; }
    }
}
