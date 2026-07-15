package com.lwe.api.dto;

import com.lwe.core.domain.NpcIntent;

import java.util.UUID;

public record NpcIntentResponse(UUID id, UUID worldId, UUID npcId, String intentType,
                                String reasoning, String status, String rejectionReason,
                                String createdAt) {

    public static NpcIntentResponse from(NpcIntent i) {
        return new NpcIntentResponse(
            i.getId(), i.getWorldId(), i.getNpcId(), i.getIntentType(),
            i.getReasoning() != null ? i.getReasoning() : "",
            i.getStatus(),
            i.getRejectionReason() != null ? i.getRejectionReason() : "",
            i.getCreatedAt().toString()
        );
    }
}
