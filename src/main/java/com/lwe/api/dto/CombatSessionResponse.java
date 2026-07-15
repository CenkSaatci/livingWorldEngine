package com.lwe.api.dto;

import com.lwe.core.domain.CombatSession;

import java.util.UUID;

public record CombatSessionResponse(UUID id, UUID worldId, String status, int round,
                                     String currentTurnEntityId, String mapId, String createdAt) {

    public static CombatSessionResponse from(CombatSession s) {
        return new CombatSessionResponse(
            s.getId(), s.getWorldId(), s.getStatus(), s.getRound(),
            s.getCurrentTurnEntityId() != null ? s.getCurrentTurnEntityId().toString() : "",
            s.getMapId() != null ? s.getMapId().toString() : "",
            s.getCreatedAt().toString()
        );
    }
}
