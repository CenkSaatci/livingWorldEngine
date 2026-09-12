package com.lwe.api.dto;

import com.lwe.core.domain.GameEntity;

import java.util.UUID;

public record EntityResponse(
    UUID id,
    UUID worldId,
    String entityType,
    String name,
    String attributesJson,
    String inventoryJson,
    String positionJson,
    String metadataJson,
    String factionId,
    String backstory,
    int age,
    String experienceLevel,
    String socialStanding,
    int experiencePoints,
    int unspentAttributePoints,
    int hpCurrent,
    int hpMax,
    int apCurrent,
    int apMax,
    String ownerUserId,
    String createdAt
) {
    public static EntityResponse from(GameEntity e) {
        return new EntityResponse(
            e.getId(), e.getWorldId(), e.getEntityType(), e.getName(),
            e.getAttributesJson(), e.getInventoryJson(),
            e.getPositionJson() != null ? e.getPositionJson() : "",
            e.getMetadataJson(),
            e.getFactionId() != null ? e.getFactionId().toString() : "",
            e.getBackstory() != null ? e.getBackstory() : "",
            e.getAge(), e.getExperienceLevel(), e.getSocialStanding(),
            e.getExperiencePoints(), e.getUnspentAttributePoints(),
            e.getHpCurrent(), e.getHpMax(), e.getApCurrent(), e.getApMax(),
            e.getOwnerUserId() != null ? e.getOwnerUserId().toString() : null,
            e.getCreatedAt().toString()
        );
    }
}
