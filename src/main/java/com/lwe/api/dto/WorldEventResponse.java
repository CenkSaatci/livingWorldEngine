package com.lwe.api.dto;

public record WorldEventResponse(long id, String eventType, String sourceEntityId,
                                   String targetEntityId, String payload, String createdAt) {}
