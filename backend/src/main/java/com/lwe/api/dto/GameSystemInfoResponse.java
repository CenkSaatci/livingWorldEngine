package com.lwe.api.dto;

import com.lwe.core.domain.GameSystem;

import java.util.UUID;

public record GameSystemInfoResponse(UUID id, String name, int version, boolean active,
                                     UUID ownerId, String visibility) {

    public static GameSystemInfoResponse from(GameSystem gs) {
        return new GameSystemInfoResponse(gs.getId(), gs.getName(), gs.getVersion(), gs.isActive(),
            gs.getOwnerId(), gs.getVisibility());
    }
}
