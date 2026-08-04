package com.lwe.api.dto;

import com.lwe.core.domain.GameSession;

import java.util.UUID;

public record GameSessionInfoResponse(UUID id, UUID worldId, UUID campaignId, String status,
                                      String startedAt, String endedAt, String createdAt) {

    public static GameSessionInfoResponse from(GameSession s) {
        return new GameSessionInfoResponse(
            s.getId(), s.getWorldId(), s.getCampaignId(), s.getStatus(),
            s.getStartedAt().toString(),
            s.getEndedAt() != null ? s.getEndedAt().toString() : "",
            s.getCreatedAt().toString()
        );
    }
}
