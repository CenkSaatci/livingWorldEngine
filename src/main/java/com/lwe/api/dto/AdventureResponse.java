package com.lwe.api.dto;

import com.lwe.core.domain.Adventure;

import java.util.UUID;

public record AdventureResponse(UUID id, UUID worldId, String name, String description,
                                 String startNodeId, String createdAt) {

    public static AdventureResponse from(Adventure a) {
        return new AdventureResponse(a.getId(), a.getWorldId(), a.getName(),
            a.getDescription() != null ? a.getDescription() : "",
            a.getStartNodeId() != null ? a.getStartNodeId().toString() : "",
            a.getCreatedAt().toString());
    }
}
