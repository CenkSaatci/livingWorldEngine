package com.lwe.api.dto;

import com.lwe.core.domain.World;

import java.util.UUID;

public record WorldInfoResponse(UUID id, String name, UUID ownerId, String visibility,
                                String settingsJson, String currentGameTime, String createdAt) {

    public static WorldInfoResponse from(World w) {
        return new WorldInfoResponse(
            w.getId(), w.getName(), w.getOwnerId(), w.getVisibility(),
            w.getSettingsJson(),
            w.getCurrentGameTime() != null ? w.getCurrentGameTime().toString() : "",
            w.getCreatedAt().toString()
        );
    }
}
