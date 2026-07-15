package com.lwe.api.dto;

import com.lwe.core.domain.WorldMember;

import java.util.UUID;

public record WorldMemberResponse(UUID id, UUID userId, String role, String joinedAt) {

    public static WorldMemberResponse from(WorldMember m) {
        return new WorldMemberResponse(m.getId(), m.getUserId(), m.getRole(), m.getJoinedAt().toString());
    }
}
