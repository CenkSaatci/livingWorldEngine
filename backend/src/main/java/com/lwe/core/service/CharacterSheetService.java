package com.lwe.core.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwe.api.dto.SheetResponse;
import com.lwe.core.domain.*;
import com.lwe.core.repository.*;
import com.lwe.core.util.EntityJson;
import com.lwe.core.util.RuleNames;
import com.lwe.core.util.WorldAccess;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Orchestriert die Character-Sheet-Berechnung:
 * Lädt Entity → World → GameSystem → rulesJson,
 * parst Attribute, evaluiert Formeln, prüft Conditionals.
 */
@Service
public class CharacterSheetService {

    private final GameEntityRepository entityRepo;
    private final WorldRepository worldRepo;
    private final WorldAccess worldAccess;
    private final ModifierService modifierService;
    private final DerivedValueService derivedValueService;
    private final LevelUpService levelUpService;
    private final RulesLoader rulesLoader;
    private final ObjectMapper objectMapper;
    private final ConditionService conditionService;
    private final com.lwe.core.util.EntityAccess entityAccess;
    private final CampaignMemberService campaignMemberService;

    private static final TypeReference<Map<String, Object>> OVERRIDE_TYPE = new TypeReference<>() {};

    public CharacterSheetService(GameEntityRepository entityRepo, WorldRepository worldRepo,
                                  WorldAccess worldAccess,
                                  com.lwe.core.util.EntityAccess entityAccess,
                                  ModifierService modifierService,
                                  DerivedValueService derivedValueService,
                                  LevelUpService levelUpService,
                                  RulesLoader rulesLoader,
                                  ObjectMapper objectMapper,
                                  ConditionService conditionService,
                                  CampaignMemberService campaignMemberService) {
        this.objectMapper = objectMapper;
        this.entityRepo = entityRepo;
        this.worldRepo = worldRepo;
        this.worldAccess = worldAccess;
        this.entityAccess = entityAccess;
        this.modifierService = modifierService;
        this.derivedValueService = derivedValueService;
        this.levelUpService = levelUpService;
        this.rulesLoader = rulesLoader;
        this.conditionService = conditionService;
        this.campaignMemberService = campaignMemberService;
    }

    public SheetResponse getSheet(UUID entityId, UUID userId) {
        return getSheet(entityId, userId, null);
    }

    public SheetResponse getSheet(UUID entityId, UUID userId, UUID campaignId) {
        var entity = entityRepo.findById(entityId)
            .orElseThrow(() -> new EntityService.EntityException("ENTITY_NOT_FOUND", "Entity not found"));

        var world = worldRepo.findById(entity.getWorldId())
            .orElseThrow(() -> new EntityService.EntityException("WORLD_NOT_FOUND", "World not found"));

        worldAccess.requireAccess(entity.getWorldId(), userId);
        // Fremde Kampagne darf das Sheet nicht mit ihrem System rechnen (finaler Audit).
        if (campaignId != null && !rulesLoader.campaignBelongsToWorld(campaignId, entity.getWorldId())) {
            throw new EntityService.EntityException("WORLD_ACCESS_DENIED",
                "Campaign does not belong to world");
        }
        // ADR-014: fremde Charaktere sehen nur Owner/DM; Leiter = Welt-DM oder Kampagnen-DM.
        boolean campaignDm = campaignId != null && campaignMemberService.isDm(campaignId, userId);
        if (entity.getOwnerUserId() != null && !entity.getOwnerUserId().equals(userId) && !campaignDm) {
            worldAccess.requireDm(entity.getWorldId(), userId);
        }

        var rules = rulesLoader.loadRules(campaignId, entity.getWorldId());
        var attributeValues = parseAttributes(entity);
        // Wenn Entity keine Attribute hat, mit Defaults aus rulesJson initialisieren
        if (attributeValues.isEmpty()) {
            attributeValues = initDefaultAttributes(rules);
        }
        // Trait-Effekte (P28-T03): Attribut-Boni vor Modifiern/Derived anwenden
        applyTraitAttributeEffects(rules, entity, attributeValues);
        var allowed = attributeValues.keySet();

        // Modifier
        var modifierFormula = rules.containsKey("modifierFormula")
            ? (String) rules.get("modifierFormula") : "";
        var modifiers = modifierService.calculateModifiers(modifierFormula, attributeValues);

        // Attribut-Min/Max aus rulesJson
        var rulesAttrs = (List<Map<String, Object>>) rules.getOrDefault("attributes", List.of());
        var attrMinMax = rulesAttrs.stream().collect(Collectors.toMap(
            a -> (String) a.get("name"),
            a -> Map.entry(
                ((Number) a.getOrDefault("min", 1)).intValue(),
                ((Number) a.getOrDefault("max", 99)).intValue()
            )
        ));

        // Attribute mit Modifiern + Min/Max (+ QA-Beschreibung als Tooltip)
        var attrDescriptions = rulesAttrs.stream()
            .filter(a -> a.get("description") instanceof String)
            .collect(Collectors.toMap(a -> (String) a.get("name"), a -> (String) a.get("description"),
                (x, y) -> x));
        var attributes = attributeValues.entrySet().stream()
            .map(e -> {
                var mm = attrMinMax.getOrDefault(e.getKey(), Map.entry(1, 99));
                return new SheetResponse.AttributeInfo(e.getKey(), e.getValue(),
                    modifiers.getOrDefault(e.getKey(), 0.0), mm.getKey(), mm.getValue(),
                    attrDescriptions.get(e.getKey()));
            })
            .collect(Collectors.toList());

        // Derived Values (ADR-014: mit Fertigkeitswerten im Formelkontext)
        var derivedRaw = (List<Map<String, Object>>) rules.getOrDefault("derived_values", List.of());
        var perCharSkills = parsePerCharacterSkills(entity);
        var skillsRaw = (List<Map<String, Object>>) rules.getOrDefault("skills", List.of());
        var skillValues = new java.util.HashMap<String, Integer>();
        for (var s : skillsRaw) {
            if (s.get("name") instanceof String n) {
                var perCharValue = RuleNames.get(perCharSkills, n);
                int v = perCharValue != null ? perCharValue
                    : (s.get("bonus") instanceof Number b ? b.intValue() : 0);
                skillValues.put(n, v);
            }
        }
        var derivedValues = derivedValueService.evaluate(derivedRaw, attributeValues,
            selectedTraits(entity), skillValues);

        // Formula Overrides aus metadata_json
        var overrides = parseOverrides(entity);
        if (!overrides.isEmpty()) {
            derivedValues = derivedValues.stream()
                .map(dv -> {
                    var overrideVal = overrides.get(dv.name());
                    if (overrideVal instanceof Number n) {
                        return new SheetResponse.DerivedValueInfo(
                            dv.name(), dv.value() + n.doubleValue(), dv.error(), dv.description());
                    }
                    return dv;
                })
                .collect(Collectors.toList());
        }

        // Trait-Effekte (P28-T03): Derived-Boni (z. B. Hohe Lebenskraft +3 hp)
        derivedValues = applyTraitDerivedEffects(rules, entity, derivedValues);

        // Skills (total = Basis + Attribut-Modifier)
        var skills = skillsRaw.stream()
            .map(s -> {
                var name = (String) s.getOrDefault("name", "");
                var globalBonus = ((Number) s.getOrDefault("bonus", 0)).intValue();
                var attrs = (List<String>) s.getOrDefault("attributes", List.of());
                var attrMod = attrs.stream()
                    .map(a -> RuleNames.getOr(modifiers, a, 0.0))
                    .mapToDouble(Double::doubleValue)
                    .sum();
                var perCharValue = RuleNames.get(perCharSkills, name);
                var effectiveBonus = perCharValue != null ? perCharValue : globalBonus;
                var total = (int) Math.round(effectiveBonus + attrMod);
                var perCharVal = perCharValue;
                var advanceCost = skillAdvanceCost(rules, s, effectiveBonus);
                var description = s.get("description") instanceof String d ? d : null;
                var kind = s.get("kind") instanceof String k ? k : null;
                return new SheetResponse.SkillInfo(name, total, perCharVal, advanceCost,
                    castingInfo(s), description, kind);
            })
            .collect(Collectors.toList());

        // Conditionals
        var conditionals = evaluateConditionals(rules, attributeValues, allowed);

        // Abilities aus rulesJson.abilities[]
        var abilities = parseAbilities(rules);

        // Level aus XP berechnen
        int level = 1;
        var gs = rulesLoader.resolveSystem(campaignId, world);
        if (gs != null) {
            level = levelUpService.getLevel(entity, gs);
        }

        int fateMax = 0;
        if (rules.get("creationBudget") instanceof Map<?, ?> budget
            && budget.get("fatePoints") instanceof Number n) {
            fateMax = n.intValue();
        }
        int fateCurrent = fatePoints(entity, fateMax);

        var activeConditions = conditionService.active(entity).stream()
            .map(c -> new SheetResponse.ConditionInfo(c.name(), c.rounds()))
            .toList();
        var conditionsRaw = rules.getOrDefault("conditions", List.of());
        var conditionCatalog = (conditionsRaw instanceof List<?> cl ? cl : List.<Object>of())
            .stream()
            .filter(Map.class::isInstance)
            .map(c -> ((Map<?, ?>) c).get("name"))
            .filter(n -> n instanceof String s2 && !s2.isBlank())
            .map(Object::toString)
            .toList();

        return new SheetResponse(
            new SheetResponse.EntityInfo(entity.getId().toString(), entity.getName(), entity.getEntityType()),
            entity.getExperiencePoints(), level, fateCurrent, fateMax, damageArmor(entity),
            attributes, derivedValues, skills, conditionals, abilities,
            activeConditions, conditionCatalog, difficultyLevels(rules)
        );
    }

    @Transactional
    public void updateProgression(UUID entityId, UUID userId, int experiencePoints) {
        var entity = entityRepo.findById(entityId)
            .orElseThrow(() -> new EntityService.EntityException("ENTITY_NOT_FOUND", "Entity not found"));
        entityAccess.checkControl(entity, userId); // Runde 1
        entity.setExperiencePoints(experiencePoints);
        entityRepo.save(entity);
    }

    private int fatePoints(GameEntity entity, int defaultMax) {
        if (entity.getMetadataJson() == null || entity.getMetadataJson().isBlank()) return defaultMax;
        try {
            var node = objectMapper.readTree(entity.getMetadataJson()).path("fate_points");
            return node.isInt() || node.isLong() ? node.asInt() : defaultMax;
        } catch (Exception e) {
            return defaultMax;
        }
    }

    private Map<String, Object> parseOverrides(GameEntity entity) {
        if (entity.getMetadataJson() == null || entity.getMetadataJson().isBlank()) return Map.of();
        try {
            var tree = objectMapper.readTree(entity.getMetadataJson());
            var overrides = tree.path("formula_overrides");
            if (overrides.isMissingNode()) return Map.of();
            return objectMapper.convertValue(overrides, OVERRIDE_TYPE);
        } catch (Exception e) {
            return Map.of();
        }
    }

    /** Ausgewählte Traits des Charakters (metadataJson.traits, z. B. ["Glück II", "Zauberer"]). */
    private List<String> selectedTraits(GameEntity entity) {
        return EntityJson.traits(objectMapper, entity.getMetadataJson());
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> traitDefs(Map<String, Object> rules) {
        var raw = rules.get("traits");
        return raw instanceof List<?> list ? (List<Map<String, Object>>) list : List.of();
    }

    /** Tier-Suffix wird ignoriert: "Hohe Lebenskraft III" wählt "Hohe Lebenskraft". */
    private boolean traitSelected(List<String> selected, String defName) {
        return RuleNames.hasTrait(selected, defName);
    }

    private void applyTraitAttributeEffects(Map<String, Object> rules, GameEntity entity,
                                            Map<String, Integer> attrs) {
        var selected = selectedTraits(entity);
        for (var def : traitDefs(rules)) {
            if (!traitSelected(selected, (String) def.getOrDefault("name", ""))) continue;
            var effects = (List<Map<String, Object>>) def.getOrDefault("effects", List.of());
            for (var e : effects) {
                var target = (String) e.getOrDefault("target", "");
                if (target.startsWith("attribute:") && "add".equals(e.get("op"))) {
                    var name = target.substring("attribute:".length());
                    // Audit P28: nur bekannte Attribute anreichern — sonst entstehen
                    // Phantom-Attribute im Sheet.
                    if (attrs.containsKey(name)) {
                        attrs.merge(name, ((Number) e.getOrDefault("value", 0)).intValue(), Integer::sum);
                    }
                }
            }
        }
    }

    private List<SheetResponse.DerivedValueInfo> applyTraitDerivedEffects(
            Map<String, Object> rules, GameEntity entity,
            List<SheetResponse.DerivedValueInfo> values) {
        var selected = selectedTraits(entity);
        var adds = new java.util.HashMap<String, Double>();
        for (var def : traitDefs(rules)) {
            if (!traitSelected(selected, (String) def.getOrDefault("name", ""))) continue;
            var effects = (List<Map<String, Object>>) def.getOrDefault("effects", List.of());
            for (var e : effects) {
                var target = (String) e.getOrDefault("target", "");
                if ("add".equals(e.get("op")) && target.startsWith("derived:")) {
                    adds.merge(target.substring("derived:".length()),
                        ((Number) e.getOrDefault("value", 0)).doubleValue(), Double::sum);
                }
            }
        }
        if (adds.isEmpty()) return values;
        return values.stream().map(dv -> {
            var add = adds.get(dv.name());
            return add != null ? new SheetResponse.DerivedValueInfo(dv.name(), dv.value() + add, dv.error(), dv.description()) : dv;
        }).collect(Collectors.toList());
    }

    /** Nächster Steigerungsschritt (P28-T04): Aktivierung oder Matrix-Zeile. */
    private Integer skillAdvanceCost(Map<String, Object> rules, Map<String, Object> skillDef,
                                     int currentValue) {
        var activation = skillDef.get("activationCost");
        if (activation instanceof Number n && currentValue <= 0) return n.intValue();
        var adv = rules.get("advancement");
        if (!(adv instanceof Map<?, ?> advMap)) return null;
        var column = skillDef.get("costColumn");
        if (!(column instanceof String col) || col.isBlank()) return null;
        if (!(advMap.get("table") instanceof List<?> rows)) return null;
        int target = currentValue + 1;
        for (var row : rows) {
            if (!(row instanceof Map<?, ?> r)) continue;
            if (!(r.get("from") instanceof Number f) || !(r.get("to") instanceof Number t)) continue;
            if (target < f.intValue() || target > t.intValue()) continue;
            if (r.get("costs") instanceof Map<?, ?> costs && costs.get(col) instanceof Number cost) {
                return cost.intValue();
            }
            return null;
        }
        return null;
    }

    private Map<String, Integer> initDefaultAttributes(Map<String, Object> rules) {        var attrs = (List<Map<String, Object>>) rules.getOrDefault("attributes", List.of());
        if (attrs.isEmpty()) return Map.of();
        var map = new java.util.LinkedHashMap<String, Integer>();
        for (var a : attrs) {
            var name = (String) a.getOrDefault("name", "");
            if (!name.isBlank()) {
                map.put(name, ((Number) a.getOrDefault("default", 10)).intValue());
            }
        }
        return map;
    }

    private GameSystem resolveGameSystem(World world) {
        return rulesLoader.loadSystem(world);
    }

    private List<SheetResponse.AbilityInfo> parseAbilities(Map<String, Object> rules) {
        var raw = (List<Map<String, Object>>) rules.getOrDefault("abilities", List.of());
        return raw.stream().map(a -> {
            var name = (String) a.getOrDefault("name", "");
            var type = (String) a.getOrDefault("type", "active");
            var cost = ((Number) a.getOrDefault("cost", 0)).intValue();
            var effect = (String) a.getOrDefault("effect", "");
            var diceExpr = (String) a.getOrDefault("diceExpression", "");
            var damageType = a.get("damageType") instanceof String dt ? dt : null;
            return new SheetResponse.AbilityInfo(name, type, cost, effect, diceExpr, damageType);
        }).collect(Collectors.toList());
    }

    /** Ruestungswert aus Entity-Metadata (P29-T04). */
    private int damageArmor(GameEntity entity) {
        if (entity.getMetadataJson() == null || entity.getMetadataJson().isBlank()) return 0;
        try {
            var node = objectMapper.readTree(entity.getMetadataJson()).path("damage_armor");
            return node.isNumber() ? node.asInt() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private Map<String, Integer> parsePerCharacterSkills(GameEntity entity) {
        return EntityJson.skills(objectMapper, entity.getSkillsJson());
    }

    /** P1: benannte Difficulty-Level aus dem Regelwerk (generisch). */
    @SuppressWarnings("unchecked")
    private List<SheetResponse.DifficultyInfo> difficultyLevels(Map<String, Object> rules) {
        if (!(rules.get("dice_mechanics") instanceof Map<?, ?> dm)) return List.of();
        if (!(dm.get("difficulties") instanceof List<?> levels)) return List.of();
        return levels.stream()
            .filter(l -> l instanceof Map<?, ?> m && m.get("name") instanceof String)
            .map(l -> {
                var m = (Map<String, Object>) l;
                var name = (String) m.get("name");
                Double mult = m.get("multiplier") instanceof Number n ? n.doubleValue() : null;
                Integer delta = m.get("delta") instanceof Number n ? n.intValue() : null;
                return new SheetResponse.DifficultyInfo(name, mult, delta);
            })
            .toList();
    }

    /** R3: casting aus rulesJson typisiert uebernehmen. */
    private SheetResponse.CastingInfo castingInfo(Map<String, Object> skillDef) {
        if (!(skillDef.get("casting") instanceof Map<?, ?> cm)) return null;
        if (!(cm.get("resource") instanceof String resource)) return null;
        var cost = cm.get("cost") instanceof Number n ? n.intValue() : 0;
        var trait = cm.get("requiresTrait") instanceof String t ? t : null;
        return new SheetResponse.CastingInfo(resource, cost, trait);
    }

    private Map<String, Integer> parseAttributes(GameEntity entity) {
        return EntityJson.attributes(objectMapper, entity.getAttributesJson());
    }

    private List<SheetResponse.ConditionalInfo> evaluateConditionals(
            Map<String, Object> rules, Map<String, Integer> attributeValues, Set<String> allowed) {
        var conditionals = (List<Map<String, Object>>) rules.getOrDefault("conditionals", List.of());
        return conditionals.stream().map(c -> {
            var name = (String) c.getOrDefault("name", "");
            var attr = (String) c.getOrDefault("attribute", "");
            var operator = (String) c.getOrDefault("operator", "gte");
            var value = ((Number) c.getOrDefault("value", 0)).intValue();
            var bonus = (String) c.getOrDefault("bonus", "");
            var target = (String) c.getOrDefault("target", "");
            var attrVal = attributeValues.getOrDefault(attr, 0);
            boolean active = switch (operator) {
                case "gt" -> attrVal > value;
                case "gte" -> attrVal >= value;
                case "lt" -> attrVal < value;
                case "lte" -> attrVal <= value;
                case "eq" -> attrVal == value;
                case "per_point" -> attrVal > value;
                default -> false;
            };
            var desc = active ? attr + " " + operator + " " + value + " → " + bonus + " auf " + target : "";
            return new SheetResponse.ConditionalInfo(name, active, desc);
        }).collect(Collectors.toList());
    }
}
