package com.lwe.core.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwe.api.dto.ProbeResponse;
import com.lwe.core.domain.GameEntity;
import com.lwe.core.domain.World;
import com.lwe.core.repository.GameEntityRepository;
import com.lwe.core.repository.GameSystemRepository;
import com.lwe.core.repository.WorldRepository;
import com.lwe.core.util.WorldAccess;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Führt systemgerechte Proben (Würfelwürfe) aus:
 * <ul>
 *   <li>d20_target (D&D): 1d20 + Modifikator ≥ Zielwert</li>
 *   <li>d100_threshold (CoC): 1d100 ≤ Fertigkeit</li>
 *   <li>d20_3attr (DSA): 3d20, je ≤ Attribut, Fehlschläge kompensieren</li>
 * </ul>
 */
@Service
public class ProbeService {

    private final GameEntityRepository entityRepo;
    private final WorldRepository worldRepo;
    private final WorldAccess worldAccess;
    private final ConditionEvaluator conditionEvaluator;
    private final ModifierService modifierService;
    private final RulesLoader rulesLoader;
    private final ObjectMapper objectMapper;
    private final ConditionService conditionService;
    private final DerivedValueService derivedValueService;
    private final EntityService entityService;
    private final RelationshipService relationshipService;

    public ProbeService(GameEntityRepository entityRepo, WorldRepository worldRepo,
                        WorldAccess worldAccess,
                        ConditionEvaluator conditionEvaluator, ModifierService modifierService,
                        RulesLoader rulesLoader, ObjectMapper objectMapper,
                        ConditionService conditionService, DerivedValueService derivedValueService,
                        EntityService entityService, RelationshipService relationshipService) {
        this.objectMapper = objectMapper;
        this.entityRepo = entityRepo;
        this.worldRepo = worldRepo;
        this.worldAccess = worldAccess;
        this.conditionEvaluator = conditionEvaluator;
        this.modifierService = modifierService;
        this.rulesLoader = rulesLoader;
        this.conditionService = conditionService;
        this.derivedValueService = derivedValueService;
        this.entityService = entityService;
        this.relationshipService = relationshipService;
    }

    public ProbeResponse executeProbe(UUID entityId, UUID userId, String skillName,
                                       int target, boolean advantage) {
        return executeProbe(entityId, userId, skillName, target, advantage, null);
    }

    public ProbeResponse executeProbe(UUID entityId, UUID userId, String skillName,
                                       int target, boolean advantage, UUID campaignId) {
        return executeProbe(entityId, userId, skillName, target, advantage, campaignId, 0);
    }

    /** difficulty (Runde 1): DSA-Probenmodifikator — positiv = erschwert,
     *  negativ = erleichtert; Schwelle = Attribut − difficulty (3W20). */
    public ProbeResponse executeProbe(UUID entityId, UUID userId, String skillName,
                                       int target, boolean advantage, UUID campaignId,
                                       int difficulty) {
        return executeProbe(entityId, userId, skillName, target, advantage, campaignId,
            new ProbeOptions(difficulty, null, 0, 0));
    }

    /**
     * Generische Proben-Optionen (P1): Difficulty-Level aus dem Regelwerk
     * ({@code dice_mechanics.difficulties}) plus Bonus-/Penalty-Würfel (d100).
     * T4: useFate = Schicksalspunkt für Bonus aus {@code fate.probeBonusPerPoint} ausgeben.
     */
    public record ProbeOptions(int difficulty, String difficultyKey, int bonusDice, int penaltyDice,
                               boolean useFate) {
        public ProbeOptions(int difficulty, String difficultyKey, int bonusDice, int penaltyDice) {
            this(difficulty, difficultyKey, bonusDice, penaltyDice, false);
        }
        public static ProbeOptions none() { return new ProbeOptions(0, null, 0, 0, false); }
    }

    public ProbeResponse executeProbe(UUID entityId, UUID userId, String skillName,
                                       int target, boolean advantage, UUID campaignId,
                                       ProbeOptions options) {
        return executeProbe(entityId, userId, skillName, target, advantage, campaignId,
            options, null, null);
    }

    /** SM-01 (ADR-013): Soziale Probe = Skill-Probe mit Beziehungs-Modifikator
     *  (socialAction aus {@code social_actions[]}, Ziel = socialTargetId). */
    public ProbeResponse executeProbe(UUID entityId, UUID userId, String skillName,
                                       int target, boolean advantage, UUID campaignId,
                                       ProbeOptions options, String socialAction, UUID socialTargetId) {
        var opts = options == null ? ProbeOptions.none() : options;
        var entity = entityRepo.findById(entityId)
            .orElseThrow(() -> new RuntimeException("ENTITY_NOT_FOUND"));
        worldAccess.requireAccess(entity.getWorldId(), userId);

        var world = worldRepo.findById(entity.getWorldId())
            .orElseThrow(() -> new RuntimeException("WORLD_NOT_FOUND"));
        var rules = rulesLoader.loadRules(campaignId, entity.getWorldId());
        var diff = resolveDifficulty(rules, opts);
        var difficulty = diff.delta();
        var probeType = resolveProbeType(rules);
        var attributes = parseAttributes(entity);
        var allowed = attributes.keySet();

        // Modifier Formel anwenden
        var modifierFormula = (String) rules.getOrDefault("modifierFormula", "");
        var modifiers = modifierService.calculateModifiers(modifierFormula, attributes);

        // Skill finden
        var skills = (List<Map<String, Object>>) rules.getOrDefault("skills", List.of());
        var skill = skills.stream()
            .filter(s -> s.getOrDefault("name", "").equals(skillName))
            .findFirst().orElse(null);

        int skillBonus = 0;
        List<String> skillAttrs = new ArrayList<>();
        if (skill != null) {
            skillBonus = ((Number) skill.getOrDefault("bonus", 0)).intValue();
            var attrs = (List<String>) skill.getOrDefault("attributes", List.of());
            if (attrs != null) skillAttrs.addAll(attrs);
        }

        // Per-Character Skill-Override aus entity.skillsJson
        var perCharSkills = parsePerCharacterSkills(entity);
        if (perCharSkills.containsKey(skillName)) {
            skillBonus = perCharSkills.get(skillName);
        }

        // Aktive Zustaende (P29-T01): Probe-Malus
        var conditionMalus = conditionService.modifier(entity, rules, "probe");

        // SM-01: Beziehungs-Modifikator fuer soziale Aktionen (Cap aus social.maxModifier).
        // Audit T7: Erst validieren (Aktion, Skill, Ziel-Welt), dann Ressourcen ausgeben.
        int socialBonus = 0;
        Map<String, Object> socialDef = null;
        if (socialAction != null && !socialAction.isBlank()) {
            socialDef = findSocialAction(rules, socialAction);
            if (socialDef == null) {
                throw new SocialException("SOCIAL_ACTION_UNKNOWN",
                    "Unknown social action: " + socialAction);
            }
            if (!skillName.equals(socialDef.get("skill"))) {
                throw new SocialException("SOCIAL_SKILL_MISMATCH",
                    "Social action requires skill: " + socialDef.get("skill"));
            }
            var socialTargetEntity = socialTargetId == null ? null
                : entityRepo.findById(socialTargetId).orElse(null);
            if (socialTargetEntity == null || !socialTargetEntity.getWorldId().equals(entity.getWorldId())) {
                throw new SocialException("SOCIAL_TARGET_INVALID", "Social target not found");
            }
            int weight = socialDef.get("relationshipWeight") instanceof Number n ? (int) Math.round(n.doubleValue()) : 1;
            var score = relationshipScore(entityId, socialTargetId, rules);
            int cap = Math.abs(rules.get("social") instanceof Map<?, ?> sc
                && sc.get("maxModifier") instanceof Number n ? n.intValue() : 3);
            socialBonus = Math.max(-cap, Math.min(cap, weight * score));
        }

        // T4: Schicksalspunkt für Probe-Bonus ausgeben (nur wenn Regelwerk fate kennt).
        int fateBonus = 0;
        if (opts.useFate() && rules.get("fate") instanceof Map<?, ?> fateCfg
            && fateCfg.get("probeBonusPerPoint") instanceof Number n && n.intValue() > 0) {
            entityService.spendFatePoint(entityId, userId, campaignId);
            fateBonus = n.intValue();
        }

        var rng = ThreadLocalRandom.current();
        List<ProbeResponse.ConditionalResult> activeConditionals;
        int total;
        int modifierTotal;
        int[] dice;
        boolean success;
        List<ProbeResponse.DieDetail> details = new ArrayList<>();

        switch (probeType) {
            case "d100_threshold": {
                var baseSkill = skillBonus + conditionMalus + skillAttrs.stream()
                    .mapToInt(a -> (int) Math.round(modifiers.getOrDefault(a, 0.0))).sum();
                // P1: Difficulty-Multiplier (z.B. CoC hard 0.5) + Bonus-/Penalty-Würfel.
                var effective = (int) Math.round(baseSkill * diff.multiplier()) + difficulty
                    + fateBonus + socialBonus;
                // Audit P1: Bonus/Penalty 1:1 verrechnen (Netto-Extras).
                var net = Math.max(opts.bonusDice(), 0) - Math.max(opts.penaltyDice(), 0);
                var extra = Math.min(2, Math.abs(net));
                var tensCount = extra + 1;
                var units = rng.nextInt(0, 10);
                var tens = new int[tensCount];
                for (int i = 0; i < tensCount; i++) {
                    tens[i] = rng.nextInt(0, 10);
                }
                // Audit P1: Kandidaten als Gesamtwerte vergleichen (00+0 = 100),
                // sonst kann ein Bonuswuerfel das Ergebnis verschlechtern.
                var candidates = Arrays.stream(tens)
                    .map(t -> { var v = t * 10 + units; return v == 0 ? 100 : v; })
                    .toArray();
                int die;
                if (net > 0) {
                    die = Arrays.stream(candidates).min().orElse(100); // Bonus: bester Wert
                } else if (net < 0) {
                    die = Arrays.stream(candidates).max().orElse(100); // Penalty: schlechtester
                } else {
                    die = candidates[0];
                }
                dice = new int[tensCount + 1];
                dice[0] = units;
                for (int i = 0; i < tensCount; i++) dice[i + 1] = tens[i];
                total = die;
                modifierTotal = effective - baseSkill;
                success = die <= effective;
                break;
            }
            case "d20_3attr": {
                // 3d20, je ≤ Attribut
                int count = Math.min(skillAttrs.size(), 3);
                var rolls = new int[count];
                int fails = 0;
                for (int i = 0; i < count; i++) {
                    rolls[i] = rng.nextInt(1, 21);
                    // Runde 1: Erschwernis/Erleichterung senkt/hebt die Attributsschwelle.
                    var attrVal = attributes.getOrDefault(skillAttrs.get(i), 10) - difficulty;
                    var ok = rolls[i] <= attrVal;
                    if (!ok) fails += rolls[i] - attrVal;
                    details.add(new ProbeResponse.DieDetail(rolls[i], skillAttrs.get(i), attrVal, ok));
                }
                dice = rolls;
                total = Arrays.stream(rolls).sum();
                modifierTotal = -fails;
                success = fails <= (skillBonus + conditionMalus + fateBonus + socialBonus);
                break;
            }
            default: { // d20_target
                var die1 = rng.nextInt(1, 21);
                var die2 = advantage ? rng.nextInt(1, 21) : die1;
                var die = advantage ? Math.max(die1, die2) : die1;
                dice = advantage ? new int[]{die1, die2} : new int[]{die};
                var attrMod = skillAttrs.stream()
                    .mapToDouble(a -> modifiers.getOrDefault(a, 0.0)).sum();
                modifierTotal = (int) Math.round(attrMod) + skillBonus + conditionMalus
                    + fateBonus + socialBonus;
                total = die + modifierTotal;
                // Audit P1: Difficulty-Delta verschiebt d20-Zielwerte (DC +/-), nicht doppelt.
                success = total >= target + difficulty;
                break;
            }
        }

        // Conditionals auswerten
        var conditionals = (List<Map<String, Object>>) rules.getOrDefault("conditionals", List.of());
        activeConditionals = conditionEvaluator.evaluate(conditionals, attributes);

        // SM-02: Erfolgs-/Fehlschlag-Zustaende der sozialen Aktion auf das Ziel anwenden.
        if (socialDef != null && socialTargetId != null) {
            applySocialEffects(socialDef, success ? "onSuccess" : "onFailure", socialTargetId, rules);
        }

        return new ProbeResponse(probeType, dice, modifierTotal, total, success, details, activeConditionals);
    }

    /** Difficulty-Level generisch: multiplier (d100) + delta (alle Systeme). */
    private record Difficulty(double multiplier, int delta) {}

    @SuppressWarnings("unchecked")
    private Difficulty resolveDifficulty(Map<String, Object> rules, ProbeOptions opts) {
        double multiplier = 1.0;
        int delta = opts.difficulty();
        if (opts.difficultyKey() != null && !opts.difficultyKey().isBlank()) {
            if (rules.get("dice_mechanics") instanceof Map<?, ?> dm
                && dm.get("difficulties") instanceof List<?> levels) {
                for (var lvl : levels) {
                    if (lvl instanceof Map<?, ?> m && opts.difficultyKey().equals(m.get("name"))) {
                        if (m.get("multiplier") instanceof Number n) multiplier = n.doubleValue();
                        if (m.get("delta") instanceof Number n) delta += n.intValue();
                        break;
                    }
                }
            }
        }
        return new Difficulty(multiplier, delta);
    }

    static String resolveProbeType(Map<String, Object> rules) {
        var explicit = (String) rules.get("probeType");
        if (explicit != null) return explicit;
        var diceMechanics = (Map<String, Object>) rules.get("dice_mechanics");
        if (diceMechanics != null) {
            var probe = (String) diceMechanics.get("probe");
            if (probe != null) {
                if (probe.startsWith("1d100")) return "d100_threshold";
                if (probe.startsWith("3d20")) return "d20_3attr";
            }
        }
        return "d20_target";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> findSocialAction(Map<String, Object> rules, String name) {
        if (!(rules.get("social_actions") instanceof List<?> actions)) return null;
        for (var a : actions) {
            if (a instanceof Map<?, ?> m && name.equals(m.get("name"))) {
                return (Map<String, Object>) m;
            }
        }
        return null;
    }

    /** Beziehungswert des Ziels aus RelationshipService, gemappt ueber social.relationshipScores. */
    private int relationshipScore(UUID actorId, UUID targetId, Map<String, Object> rules) {
        if (targetId == null) return 0;
        String relation = null;
        for (var rel : relationshipService.getRelationships(actorId)) {
            var other = rel.getEntityAId().equals(actorId) ? rel.getEntityBId() : rel.getEntityAId();
            if (!other.equals(targetId)) continue;
            relation = rel.getRelationship();
            break;
        }
        if (relation == null) return 0;
        if (rules.get("social") instanceof Map<?, ?> sc
            && sc.get("relationshipScores") instanceof Map<?, ?> scores
            && scores.get(relation) instanceof Number n) {
            return (int) Math.round(n.doubleValue());
        }
        return 0;
    }

    @SuppressWarnings("unchecked")
    private void applySocialEffects(Map<String, Object> socialDef, String key, UUID targetId,
                                    Map<String, Object> rules) {
        if (!(socialDef.get(key) instanceof List<?> effects)) return;
        for (var e : effects) {
            if (!(e instanceof Map<?, ?> m) || !(m.get("condition") instanceof String condition)) continue;
            Integer rounds = m.get("rounds") instanceof Number n ? n.intValue() : null;
            entityService.applyCondition(targetId, condition, rounds, rules);
        }
    }

    private Map<String, Integer> parsePerCharacterSkills(GameEntity entity) {
        if (entity.getSkillsJson() == null || entity.getSkillsJson().isBlank()) return Map.of();
        try {
            return objectMapper.readValue(entity.getSkillsJson(), new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private Map<String, Integer> parseAttributes(GameEntity entity) {
        if (entity.getAttributesJson() == null || entity.getAttributesJson().isBlank()) return Map.of();
        try {
            return objectMapper.readValue(entity.getAttributesJson(), new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    /** B3: Zauber/Liturgien wirken — Probe + Ressourcen-Abzug (AsP/KaP). */
    @Transactional
    public CastResult cast(UUID entityId, UUID userId, String skillName, UUID campaignId) {
        return cast(entityId, userId, skillName, campaignId, null, null);
    }

    /** F4: d20-Systeme brauchen eine echte Schwelle (Default 10 statt 0 = Auto-Erfolg);
     *  DSA nutzt difficulty (Default 0). */
    public CastResult cast(UUID entityId, UUID userId, String skillName, UUID campaignId,
                           Integer target, Integer difficulty) {
        var entity = entityRepo.findById(entityId)
            .orElseThrow(() -> new CastException("CAST_ENTITY_NOT_FOUND", "Entity not found"));
        worldAccess.requireAccess(entity.getWorldId(), userId);
        var rules = rulesLoader.loadRules(campaignId, entity.getWorldId());

        var skills = (List<Map<String, Object>>) rules.getOrDefault("skills", List.of());
        var skill = skills.stream()
            .filter(s -> s.getOrDefault("name", "").equals(skillName))
            .findFirst()
            .orElseThrow(() -> new CastException("CAST_SKILL_NOT_FOUND", "Skill not found"));
        var casting = (Map<String, Object>) skill.get("casting");
        if (casting == null)
            throw new CastException("CAST_NOT_CASTABLE", "Skill is not castable");
        // P1: Ressourcen sind generisch (asp/kap/mp/slot_1/…); max kommt aus
        // dem abgeleiteten Wert gleichen Namens, sonst ist der Zauber nicht nutzbar.
        var resource = (String) casting.getOrDefault("resource", "asp");
        if (resource == null || !resource.matches("[a-z][a-z0-9_]{0,30}"))
            throw new CastException("CAST_NOT_CASTABLE", "Unknown cast resource");
        var cost = ((Number) casting.getOrDefault("cost", 0)).intValue();
        if (cost < 1)
            throw new CastException("CAST_NOT_CASTABLE", "Cast has no cost");

        var requiredTrait = (String) casting.get("requiresTrait");
        if (requiredTrait != null && !requiredTrait.isBlank() && !entityTraits(entity).contains(requiredTrait))
            throw new CastException("CAST_MISSING_TRAIT", "Missing required trait: " + requiredTrait);

        var attributes = parseAttributes(entity);
        var max = derivedValueService
            .evaluate((List<Map<String, Object>>) rules.getOrDefault("derived_values", List.of()),
                attributes, entityTraits(entity)).stream()
            .filter(dv -> dv.name().equals(resource))
            .map(dv -> (int) Math.round(dv.value()))
            .findFirst().orElse(0);
        var meta = readMeta(entity);
        var key = resource + "_current";
        var current = meta.has(key) ? meta.get(key).asInt(max) : max;
        if (current < cost)
            throw new CastException("CAST_INSUFFICIENT_RESOURCE",
                "Not enough " + resource.toUpperCase() + " (" + current + "/" + cost + ")");

        var probe = executeProbe(entityId, userId, skillName,
            target != null ? target : 10, false, campaignId,
            difficulty != null ? difficulty : 0);
        var remaining = current - cost;
        meta.put(key, remaining);
        entity.setMetadataJson(meta.toString());
        entityRepo.save(entity);
        return new CastResult(probe, resource, cost, remaining, max);
    }

    public record CastResult(ProbeResponse probe, String resource, int cost,
                             int resourceRemaining, int resourceMax) {}

    /** SM-01: unbekannte soziale Aktion o. ae. */
    public static class SocialException extends RuntimeException {
        private final String errorCode;
        public SocialException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }
        public String getErrorCode() { return errorCode; }
    }

    public static class CastException extends RuntimeException {
        private final String errorCode;
        public CastException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }
        public String getErrorCode() { return errorCode; }
    }

    private List<String> entityTraits(GameEntity entity) {
        if (entity.getMetadataJson() == null || entity.getMetadataJson().isBlank()) return List.of();
        try {
            var node = objectMapper.readTree(entity.getMetadataJson()).path("traits");
            if (!node.isArray()) return List.of();
            var out = new java.util.ArrayList<String>();
            node.forEach(n -> { if (n.isTextual()) out.add(n.asText()); });
            return out;
        } catch (Exception e) {
            return List.of();
        }
    }

    private com.fasterxml.jackson.databind.node.ObjectNode readMeta(GameEntity entity) {
        if (entity.getMetadataJson() == null || entity.getMetadataJson().isBlank())
            return objectMapper.createObjectNode();
        try {
            var parsed = objectMapper.readTree(entity.getMetadataJson());
            return parsed.isObject() ? (com.fasterxml.jackson.databind.node.ObjectNode) parsed
                : objectMapper.createObjectNode();
        } catch (Exception e) {
            return objectMapper.createObjectNode();
        }
    }
}
