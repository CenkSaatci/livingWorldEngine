package com.lwe.api.dto;

import com.lwe.core.domain.User;
import com.lwe.core.domain.WorldMember;

import java.util.UUID;

/**
 * Welt-Mitglied inkl. Anzeigename. {@code username}/{@code email} sind optional,
 * damit Aufrufer ohne User-Lookup (und alte Clients) kompatibel bleiben.
 */
public record WorldMemberResponse(UUID id, UUID userId, String username, String email,
                                  String role, String joinedAt) {

    public static WorldMemberResponse from(WorldMember m) {
        return from(m, null);
    }

    public static WorldMemberResponse from(WorldMember m, User user) {
        return new WorldMemberResponse(m.getId(), m.getUserId(),
            user != null ? user.getUsername() : null,
            user != null ? user.getEmail() : null,
            m.getRole(), m.getJoinedAt().toString());
    }
}
