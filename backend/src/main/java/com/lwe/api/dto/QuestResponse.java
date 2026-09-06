package com.lwe.api.dto;

import com.lwe.core.domain.Quest;

import java.util.UUID;

public record QuestResponse(UUID id, UUID worldId, String title, String description,
                             String type, String status, String objectives, String rewards,
                             boolean aiGenerated, String createdAt) {

    public static QuestResponse from(Quest q) {
        return new QuestResponse(q.getId(), q.getWorldId(), q.getTitle(),
            q.getDescription() != null ? q.getDescription() : "",
            q.getType(), q.getStatus(), q.getObjectives(), q.getRewards(),
            q.isAiGenerated(), q.getCreatedAt().toString());
    }
}
