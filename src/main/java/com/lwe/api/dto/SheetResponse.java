package com.lwe.api.dto;

import java.util.List;
import java.util.Map;

public record SheetResponse(
    EntityInfo entity,
    List<AttributeInfo> attributes,
    List<DerivedValueInfo> derivedValues,
    List<SkillInfo> skills,
    List<ConditionalInfo> conditionals
) {
    public record EntityInfo(String id, String name, String entityType) {}
    public record AttributeInfo(String name, int value, double modifier) {}
    public record DerivedValueInfo(String name, double value) {}
    public record SkillInfo(String name, int total) {}
    public record ConditionalInfo(String name, boolean active, String description) {}
}
