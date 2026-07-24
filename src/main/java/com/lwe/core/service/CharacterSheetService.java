package com.lwe.core.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwe.api.dto.SheetResponse;
import com.lwe.core.domain.*;
import com.lwe.core.repository.*;
import com.lwe.core.util.WorldAccess;
import org.springframework.stereotype.Service;

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
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final TypeReference<Map<String, Integer>> ATTR_MAP_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<Map<String, Object>>> LIST_MAP_TYPE = new TypeReference<>() {};

    public CharacterSheetService(GameEntityRepository entityRepo, WorldRepository worldRepo,
                                  GameSystemRepository systemRepo, WorldAccess worldAccess,
                                  ModifierService modifierService,
                                  DerivedValueService derivedValueService) {
        this.entityRepo = entityRepo;
        this.worldRepo = worldRepo;
        this.systemRepo = systemRepo;
        this.worldAccess = worldAccess;
        this.modifierService = modifierService;
        this.derivedValueService = derivedValueService;
    }

    public SheetResponse getSheet(UUID entityId, UUID userId) {
        var entity = entityRepo.findById(entityId)
            .orElseThrow(() -> new RuntimeException("ENTITY_NOT_FOUND"));

        var world = worldRepo.findById(entity.getWorldId())
            .orElseThrow(() -> new RuntimeException("WORLD_NOT_FOUND"));

        worldAccess.requireAccess(entity.getWorldId(), userId);

        var rules = parseRules(world);
        var attributeValues = parseAttributes(entity);
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

        // Skills (total = Basis + Attribut-Modifier)
        var skillsRaw = (List<Map<String, Object>>) rules.getOrDefault("skills", List.of());
        var skills = skillsRaw.stream()
            .map(s -> {
                var name = (String) s.getOrDefault("name", "");
                var bonus = ((Number) s.getOrDefault("bonus", 0)).intValue();
                var attrs = (List<String>) s.getOrDefault("attributes", List.of());
                var attrMod = attrs.stream()
                    .map(a -> modifiers.getOrDefault(a, 0.0))
                    .mapToDouble(Double::doubleValue)
                    .sum();
                var total = (int) Math.round(bonus + attrMod);
                return new SheetResponse.SkillInfo(name, total);
            })
            .collect(Collectors.toList());

        // Conditionals
        var conditionals = evaluateConditionals(rules, attributeValues, allowed);

        return new SheetResponse(
            new SheetResponse.EntityInfo(entity.getId().toString(), entity.getName(), entity.getEntityType()),
            entity.getExperiencePoints(), 0, attributes, derivedValues, skills, conditionals
        );
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
