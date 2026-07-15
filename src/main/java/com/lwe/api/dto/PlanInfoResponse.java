package com.lwe.api.dto;

import java.util.UUID;

public record PlanInfoResponse(UUID planId, String planName, int maxWorlds,
                                int maxMembersPerWorld, boolean aiModeAllowed, String expiresAt) {}
