package com.lwe.api.dto;

import java.util.List;
import java.util.Map;

public record SheetResponse(
    EntityInfo entity,
    int experiencePoints,
    int level,
    int fatePoints,
    int fateMax,
    List<AttributeInfo> attributes,
    List<DerivedValueInfo> derivedValues,
    List<SkillInfo> skills,
    List<ConditionalInfo> conditionals,
    List<AbilityInfo> abilities,
    List<ConditionInfo> activeConditions,
    List<String> conditionCatalog
) {
    public record EntityInfo(String id, String name, String entityType) {}
    public record AttributeInfo(String name, int value, double modifier, int min, int max) {}
    public record DerivedValueInfo(String name, double value, String error) {}
    public record SkillInfo(String name, int total, Integer perCharacterValue, Integer advanceCost) {}
    public record ConditionalInfo(String name, boolean active, String description) {}
    public record AbilityInfo(String name, String type, int apCost, String effect, String diceExpression) {}
    public record ConditionInfo(String name, Integer rounds) {}
}
