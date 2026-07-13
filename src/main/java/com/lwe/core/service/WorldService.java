package com.lwe.core.service;

import com.lwe.core.domain.GameSystem;
import com.lwe.core.domain.World;
import com.lwe.core.domain.WorldMember;
import com.lwe.core.repository.GameSystemRepository;
import com.lwe.core.repository.WorldMemberRepository;
import com.lwe.core.repository.WorldRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class WorldService {

    private final WorldRepository worldRepo;
    private final WorldMemberRepository memberRepo;
    private final GameSystemRepository gameSystemRepo;

    public WorldService(WorldRepository worldRepo, WorldMemberRepository memberRepo,
                        GameSystemRepository gameSystemRepo) {
        this.worldRepo = worldRepo;
        this.memberRepo = memberRepo;
        this.gameSystemRepo = gameSystemRepo;
    }

    @Transactional
    public World create(String name, UUID ownerId, UUID gameSystemId, String settingsJson) {
        if (gameSystemId != null) {
            gameSystemRepo.findById(gameSystemId)
                .filter(GameSystem::isActive)
                .orElseThrow(() -> new WorldException("WORLD_GAME_SYSTEM_INACTIVE",
                    "Game system not found or inactive"));
        }
        var world = new World(name, ownerId, gameSystemId, settingsJson);
        return worldRepo.save(world);
    }

    /**
     * Welten, die der User besitzt.
     */
    public List<World> listOwned(UUID userId) {
        return worldRepo.findByOwnerIdAndActiveTrue(userId);
    }

    /**
     * Welten, auf die der User Zugriff hat (Owner + Member).
     */
    public List<World> listAccessible(UUID userId) {
        var memberWorldIds = memberRepo.findWorldIdsByUserId(userId);
        if (memberWorldIds.isEmpty()) {
            return worldRepo.findByOwnerIdAndActiveTrue(userId);
        }
        var owned = worldRepo.findByOwnerIdAndActiveTrue(userId);
        var joined = worldRepo.findAllById(memberWorldIds).stream()
            .filter(World::isActive)
            .filter(w -> !w.getOwnerId().equals(userId)) // nicht doppelt
            .toList();
        return java.util.stream.Stream.concat(owned.stream(), joined.stream()).toList();
    }

    /**
     * Welt-Detail — Owner und Mitglieder haben Zugriff.
     */
    public World getById(UUID worldId, UUID userId) {
        var world = worldRepo.findById(worldId)
            .orElseThrow(() -> new WorldException("WORLD_NOT_FOUND", "World not found"));
        if (!world.getOwnerId().equals(userId) && !memberRepo.existsByWorldIdAndUserId(worldId, userId)) {
            throw new WorldException("WORLD_ACCESS_DENIED", "Access denied");
        }
        return world;
    }

    @Transactional
    public World update(UUID worldId, UUID userId, String name, String settingsJson) {
        var world = getById(worldId, userId);
        if (!world.getOwnerId().equals(userId)) {
            throw new WorldException("WORLD_OWNER_REQUIRED", "Only the owner may update this world");
        }
        if (name != null) world.setName(name);
        if (settingsJson != null) world.setSettingsJson(settingsJson);
        return worldRepo.save(world);
    }

    @Transactional
    public void delete(UUID worldId, UUID userId) {
        var world = requireOwner(worldId, userId);
        world.setActive(false);
        worldRepo.save(world);
    }

    @Transactional
    public WorldMember addMember(UUID worldId, UUID userId, UUID memberUserId, String role) {
        requireOwner(worldId, userId); // nur Owner darf einladen
        if (memberRepo.existsByWorldIdAndUserId(worldId, memberUserId)) {
            throw new WorldException("WORLD_MEMBER_ALREADY", "User is already a member");
        }
        var member = new WorldMember(worldId, memberUserId, role);
        return memberRepo.save(member);
    }

    @Transactional
    public void removeMember(UUID worldId, UUID userId, UUID memberUserId) {
        requireOwner(worldId, userId);
        var member = memberRepo.findByWorldIdAndUserId(worldId, memberUserId)
            .orElseThrow(() -> new WorldException("ENTITY_NOT_FOUND", "Member not found"));
        memberRepo.delete(member);
    }

    public List<WorldMember> listMembers(UUID worldId, UUID userId) {
        requireOwner(worldId, userId);
        return memberRepo.findByWorldId(worldId);
    }

    private World requireOwner(UUID worldId, UUID userId) {
        var world = worldRepo.findById(worldId)
            .orElseThrow(() -> new WorldException("WORLD_NOT_FOUND", "World not found"));
        if (!world.getOwnerId().equals(userId)) {
            throw new WorldException("WORLD_OWNER_REQUIRED", "Only the owner may perform this action");
        }
        return world;
    }

    public static class WorldException extends RuntimeException {
        private final String errorCode;
        public WorldException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }
        public String getErrorCode() { return errorCode; }
    }
}