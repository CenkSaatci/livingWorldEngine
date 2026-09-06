package com.lwe.api.dto;

import java.util.UUID;

public record ParticipantResponse(
    UUID id, UUID entityId, String name, int initiative,
    int apCurrent, int apMax, int hpCurrent, int hpMax, String side
) {}
