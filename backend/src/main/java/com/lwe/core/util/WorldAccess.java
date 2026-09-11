package com.lwe.core.util;

import com.lwe.core.repository.WorldMemberRepository;
import com.lwe.core.repository.WorldRepository;
import org.springframework.stereotype.Component;

import com.lwe.core.domain.World;

import java.util.UUID;

@Component
public class WorldAccess {

    private final WorldMemberRepository memberRepo;
    private final WorldRepository worldRepo;

    public WorldAccess(WorldMemberRepository memberRepo, WorldRepository worldRepo) {
        this.memberRepo = memberRepo;
        this.worldRepo = worldRepo;
    }

    /** Schreiben/Spielen: Owner oder (ausser PRIVATE) Mitglieder; PUBLIC allein reicht nicht. */
    public void requireAccess(UUID worldId, UUID userId) {
        var world = load(worldId);
        if (world.getOwnerId().equals(userId)) return;
        if ("PRIVATE".equals(world.getVisibility())) throw denied();
        if (!memberRepo.existsByWorldIdAndUserId(worldId, userId)) throw denied();
    }

    /** Lesen (T33-02): Owner, Mitglieder (ausser PRIVATE) oder PUBLIC-Welten. */
    public void requireRead(UUID worldId, UUID userId) {
        var world = load(worldId);
        if (world.getOwnerId().equals(userId)) return;
        if ("PUBLIC".equals(world.getVisibility())) return;
        if (!"PRIVATE".equals(world.getVisibility())
            && memberRepo.existsByWorldIdAndUserId(worldId, userId)) return;
        throw denied();
    }

    private World load(UUID worldId) {
        var world = worldRepo.findById(worldId)
            .orElseThrow(() -> new WorldAccessException("WORLD_NOT_FOUND", "World not found"));
        // Geloeschte (soft-deleted) Welten sind gesperrt (Audit P27).
        if (!world.isActive()) {
            throw new WorldAccessException("WORLD_ACCESS_DENIED", "World is deleted");
        }
        return world;
    }

    private static WorldAccessException denied() {
        return new WorldAccessException("WORLD_ACCESS_DENIED", "Access denied");
    }

    /** DM-Rechte: Welt-Owner oder Mitglied mit Rolle DM. */
    public void requireDm(UUID worldId, UUID userId) {
        var world = worldRepo.findById(worldId)
            .orElseThrow(() -> new WorldAccessException("WORLD_NOT_FOUND", "World not found"));
        if (world.getOwnerId().equals(userId)) return;
        if (!world.isActive()) {
            throw new WorldAccessException("WORLD_ACCESS_DENIED", "World is deleted");
        }
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
