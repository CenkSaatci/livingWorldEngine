package com.lwe.api.dto;

import com.lwe.core.domain.EntityEvent;

import java.util.UUID;

public record EntityEventResponse(long id, String entityType, UUID entityId, String eventType,
                                  String title, String description, int importance, String createdAt) {

    public static EntityEventResponse from(EntityEvent e) {
        return new EntityEventResponse(
            e.getId(), e.getEntityType(), e.getEntityId(), e.getEventType(),
            e.getTitle(), e.getDescription() != null ? e.getDescription() : "",
            e.getImportance(), e.getCreatedAt().toString()
        );
    }
}
