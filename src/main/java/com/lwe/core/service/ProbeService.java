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

    public ProbeService(GameEntityRepository entityRepo, WorldRepository worldRepo,
                        WorldAccess worldAccess,
                        ConditionEvaluator conditionEvaluator, ModifierService modifierService,
                        RulesLoader rulesLoader, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.entityRepo = entityRepo;
        this.worldRepo = worldRepo;
        this.worldAccess = worldAccess;
        this.conditionEvaluator = conditionEvaluator;
        this.modifierService = modifierService;
        this.rulesLoader = rulesLoader;
    }

    public ProbeResponse executeProbe(UUID entityId, UUID userId, String skillName,
                                       int target, boolean advantage) {
        var entity = entityRepo.findById(entityId)
            .orElseThrow(() -> new RuntimeException("ENTITY_NOT_FOUND"));
        worldAccess.requireAccess(entity.getWorldId(), userId);

        var world = worldRepo.findById(entity.getWorldId())
            .orElseThrow(() -> new RuntimeException("WORLD_NOT_FOUND"));
        var rules = rulesLoader.loadRules(world);
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

        var rng = ThreadLocalRandom.current();
        List<ProbeResponse.ConditionalResult> activeConditionals;
        int total;
        int modifierTotal;
        int[] dice;
        boolean success;
        List<ProbeResponse.DieDetail> details = new ArrayList<>();

        switch (probeType) {
            case "d100_threshold": {
                var die = rng.nextInt(1, 101);
                dice = new int[]{die};
                total = die;
                modifierTotal = 0;
                success = die <= (skillBonus + skillAttrs.stream()
                    .mapToInt(a -> (int) Math.round(modifiers.getOrDefault(a, 0.0))).sum());
                break;
            }
            case "d20_3attr": {
                // 3d20, je ≤ Attribut
                int count = Math.min(skillAttrs.size(), 3);
                var rolls = new int[count];
                int fails = 0;
                for (int i = 0; i < count; i++) {
                    rolls[i] = rng.nextInt(1, 21);
                    var attrVal = attributes.getOrDefault(skillAttrs.get(i), 10);
                    var ok = rolls[i] <= attrVal;
                    if (!ok) fails += rolls[i] - attrVal;
                    details.add(new ProbeResponse.DieDetail(rolls[i], skillAttrs.get(i), attrVal, ok));
                }
                dice = rolls;
                total = Arrays.stream(rolls).sum();
                modifierTotal = -fails;
                success = fails <= skillBonus;
                break;
            }
            default: { // d20_target
                var die1 = rng.nextInt(1, 21);
                var die2 = advantage ? rng.nextInt(1, 21) : die1;
                var die = advantage ? Math.max(die1, die2) : die1;
                dice = advantage ? new int[]{die1, die2} : new int[]{die};
                var attrMod = skillAttrs.stream()
                    .mapToDouble(a -> modifiers.getOrDefault(a, 0.0)).sum();
                modifierTotal = (int) Math.round(attrMod) + skillBonus;
                total = die + modifierTotal;
                success = total >= target;
                break;
            }
        }

        // Conditionals auswerten
        var conditionals = (List<Map<String, Object>>) rules.getOrDefault("conditionals", List.of());
        activeConditionals = conditionEvaluator.evaluate(conditionals, attributes);

        return new ProbeResponse(probeType, dice, modifierTotal, total, success, details, activeConditionals);
    }

    private String resolveProbeType(Map<String, Object> rules) {
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
}
