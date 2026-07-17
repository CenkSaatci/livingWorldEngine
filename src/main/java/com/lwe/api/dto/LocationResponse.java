package com.lwe.api.dto;

import com.lwe.core.domain.Location;

import java.util.UUID;

public record LocationResponse(UUID id, UUID regionId, String type, String name,
                                String description, int population, int wealth, String positionJson, String createdAt) {

    public static LocationResponse from(Location loc) {
        return new LocationResponse(loc.getId(), loc.getRegionId(), loc.getType(), loc.getName(),
            loc.getDescription() != null ? loc.getDescription() : "",
            loc.getPopulation(), loc.getWealth(), loc.getPositionJson(), loc.getCreatedAt().toString());
    }
}
