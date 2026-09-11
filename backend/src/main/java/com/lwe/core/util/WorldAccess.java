package com.lwe.core.util;

import com.lwe.core.repository.WorldMemberRepository;
import com.lwe.core.repository.WorldRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class WorldAccess {

    private final WorldMemberRepository memberRepo;
    private final WorldRepository worldRepo;

    public WorldAccess(WorldMemberRepository memberRepo, WorldRepository worldRepo) {
        this.memberRepo = memberRepo;
        this.worldRepo = worldRepo;
    }

    public void requireAccess(UUID worldId, UUID userId) {
        var world = worldRepo.findById(worldId)
            .orElseThrow(() -> new WorldAccessException("WORLD_NOT_FOUND", "World not found"));
        if (!world.getOwnerId().equals(userId) && !memberRepo.existsByWorldIdAndUserId(worldId, userId)) {
            throw new WorldAccessException("WORLD_ACCESS_DENIED", "Access denied");
        }
        // Geloeschte (soft-deleted) Welten sind gesperrt (Audit P27).
        if (!world.isActive()) {
            throw new WorldAccessException("WORLD_ACCESS_DENIED", "World is deleted");
        }
    }

    /** DM-Rechte: Welt-Owner oder Mitglied mit Rolle DM. */
    public void requireDm(UUID worldId, UUID userId) {
        var world = worldRepo.findById(worldId)
            .orElseThrow(() -> new WorldAccessException("WORLD_NOT_FOUND", "World not found"));
        if (world.getOwnerId().equals(userId)) return;
        var member = memberRepo.findByWorldIdAndUserId(worldId, userId);
        if (member.isEmpty() || !"DM".equals(member.get().getRole()))
            throw new WorldAccessException("WORLD_ACCESS_DENIED", "DM access required");
    }

    public static class WorldAccessException extends RuntimeException {
        private final String errorCode;
        public WorldAccessException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }
        public String getErrorCode() { return errorCode; }
    }
}
