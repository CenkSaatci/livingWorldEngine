package com.lwe.api.dto;

import com.lwe.core.domain.Region;
import java.util.UUID;

public record RegionResponse(
    UUID id, UUID worldId, String name, String description,
    String history, int dangerLevel, String climate,
    String resources, String factions, int population,
    String polygonPoints, String createdAt
) {
    public static RegionResponse from(Region r) {
        return new RegionResponse(
            r.getId(), r.getWorldId(), r.getName(),
            r.getDescription() != null ? r.getDescription() : "",
            r.getHistory() != null ? r.getHistory() : "",
            r.getDangerLevel(), r.getClimate(),
            r.getResources(), r.getFactions(),
            r.getPopulation(),
            r.getPolygonPoints() != null ? r.getPolygonPoints() : null,
            r.getCreatedAt().toString()
        );
    }
}
