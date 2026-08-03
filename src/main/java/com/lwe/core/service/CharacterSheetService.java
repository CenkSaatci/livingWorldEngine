package com.lwe.core.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwe.api.dto.SheetResponse;
import com.lwe.core.domain.*;
import com.lwe.core.repository.*;
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
    private final GameSystemRepository systemRepo;
    private final WorldAccess worldAccess;
    private final ModifierService modifierService;
    private final DerivedValueService derivedValueService;
    private final LevelUpService levelUpService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final TypeReference<Map<String, Integer>> ATTR_MAP_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<Map<String, Object>>> LIST_MAP_TYPE = new TypeReference<>() {};
    private static final TypeReference<Map<String, Object>> OVERRIDE_TYPE = new TypeReference<>() {};

    public CharacterSheetService(GameEntityRepository entityRepo, WorldRepository worldRepo,
                                  GameSystemRepository systemRepo, WorldAccess worldAccess,
                                  ModifierService modifierService,
                                  DerivedValueService derivedValueService,
                                  LevelUpService levelUpService) {
        this.entityRepo = entityRepo;
        this.worldRepo = worldRepo;
        this.systemRepo = systemRepo;
        this.worldAccess = worldAccess;
        this.modifierService = modifierService;
        this.derivedValueService = derivedValueService;
        this.levelUpService = levelUpService;
    }

    public SheetResponse getSheet(UUID entityId, UUID userId) {
        var entity = entityRepo.findById(entityId)
            .orElseThrow(() -> new RuntimeException("ENTITY_NOT_FOUND"));

        var world = worldRepo.findById(entity.getWorldId())
            .orElseThrow(() -> new RuntimeException("WORLD_NOT_FOUND"));

        worldAccess.requireAccess(entity.getWorldId(), userId);

        var rules = parseRules(world);
        var attributeValues = parseAttributes(entity);
        // Wenn Entity keine Attribute hat, mit Defaults aus rulesJson initialisieren
        if (attributeValues.isEmpty()) {
            attributeValues = initDefaultAttributes(rules);
        }
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

        // Attribute mit Modifiern + Min/Max
        var attributes = attributeValues.entrySet().stream()
            .map(e -> {
                var mm = attrMinMax.getOrDefault(e.getKey(), Map.entry(1, 99));
                return new SheetResponse.AttributeInfo(e.getKey(), e.getValue(),
                    modifiers.getOrDefault(e.getKey(), 0.0), mm.getKey(), mm.getValue());
            })
            .collect(Collectors.toList());

        // Derived Values
        var derivedRaw = (List<Map<String, Object>>) rules.getOrDefault("derived_values", List.of());
        var derivedValues = derivedValueService.evaluate(derivedRaw, attributeValues);

        // Formula Overrides aus metadata_json
        var overrides = parseOverrides(entity);
        if (!overrides.isEmpty()) {
            derivedValues = derivedValues.stream()
                .map(dv -> {
                    var overrideVal = overrides.get(dv.name());
                    if (overrideVal instanceof Number n) {
                        return new SheetResponse.DerivedValueInfo(
                            dv.name(), dv.value() + n.doubleValue());
                    }
                    return dv;
                })
                .collect(Collectors.toList());
        }

        // Skills (total = Basis + Attribut-Modifier)
        var perCharSkills = parsePerCharacterSkills(entity);
        var skillsRaw = (List<Map<String, Object>>) rules.getOrDefault("skills", List.of());
        var skills = skillsRaw.stream()
            .map(s -> {
                var name = (String) s.getOrDefault("name", "");
                var globalBonus = ((Number) s.getOrDefault("bonus", 0)).intValue();
                var attrs = (List<String>) s.getOrDefault("attributes", List.of());
                var attrMod = attrs.stream()
                    .map(a -> modifiers.getOrDefault(a, 0.0))
                    .mapToDouble(Double::doubleValue)
                    .sum();
                var effectiveBonus = perCharSkills.containsKey(name)
                    ? perCharSkills.get(name) : globalBonus;
                var total = (int) Math.round(effectiveBonus + attrMod);
                var perCharVal = perCharSkills.get(name);
                return new SheetResponse.SkillInfo(name, total, perCharVal);
            })
            .collect(Collectors.toList());

        // Conditionals
        var conditionals = evaluateConditionals(rules, attributeValues, allowed);

        // Abilities aus rulesJson.abilities[]
        var abilities = parseAbilities(rules);

        // Level aus XP berechnen
        int level = 1;
        var gs = resolveGameSystem(world);
        if (gs != null) {
            level = levelUpService.getLevel(entity, gs);
        }

        return new SheetResponse(
            new SheetResponse.EntityInfo(entity.getId().toString(), entity.getName(), entity.getEntityType()),
            entity.getExperiencePoints(), level, attributes, derivedValues, skills, conditionals, abilities
        );
    }

    @Transactional
    public void updateProgression(UUID entityId, UUID userId, int experiencePoints) {
        var entity = entityRepo.findById(entityId)
            .orElseThrow(() -> new RuntimeException("ENTITY_NOT_FOUND"));
        worldAccess.requireAccess(entity.getWorldId(), userId);
        entity.setExperiencePoints(experiencePoints);
        entityRepo.save(entity);
    }

    private Map<String, Object> parseRules(World world) {
        if (world.getGameSystemId() == null) return Map.of();
        var system = systemRepo.findById(world.getGameSystemId()).orElse(null);
        if (system == null || system.getRulesJson() == null || system.getRulesJson().isBlank()) return Map.of();
        try {
            return objectMapper.readValue(system.getRulesJson(), new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of();
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

    private Map<String, Integer> initDefaultAttributes(Map<String, Object> rules) {
        var attrs = (List<Map<String, Object>>) rules.getOrDefault("attributes", List.of());
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
        if (world.getGameSystemId() == null) return null;
        return systemRepo.findById(world.getGameSystemId()).orElse(null);
    }

    private List<SheetResponse.AbilityInfo> parseAbilities(Map<String, Object> rules) {
        var raw = (List<Map<String, Object>>) rules.getOrDefault("abilities", List.of());
        return raw.stream().map(a -> {
            var name = (String) a.getOrDefault("name", "");
            var type = (String) a.getOrDefault("type", "active");
            var cost = ((Number) a.getOrDefault("cost", 0)).intValue();
            var effect = (String) a.getOrDefault("effect", "");
            var diceExpr = (String) a.getOrDefault("diceExpression", "");
            return new SheetResponse.AbilityInfo(name, type, cost, effect, diceExpr);
        }).collect(Collectors.toList());
    }

    private Map<String, Integer> parsePerCharacterSkills(GameEntity entity) {
        if (entity.getSkillsJson() == null || entity.getSkillsJson().isBlank()) return Map.of();
        try {
            return objectMapper.readValue(entity.getSkillsJson(), ATTR_MAP_TYPE);
        } catch (Exception e) {
            return Map.of();
        }
    }

    private Map<String, Integer> parseAttributes(GameEntity entity) {
        if (entity.getAttributesJson() == null || entity.getAttributesJson().isBlank() || entity.getAttributesJson().equals("{}")) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(entity.getAttributesJson(), ATTR_MAP_TYPE);
        } catch (Exception e) {
            return Map.of();
        }
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
