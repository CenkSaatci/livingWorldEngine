package com.lwe.api.dto;

import java.util.UUID;

public record WorldEventResponse(long id, String eventType, String campaignId, String sourceEntityId,
                                   String targetEntityId, String payload, String createdAt) {}
