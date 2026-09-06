package com.lwe.core.service;

import com.lwe.core.domain.WorldInvite;
import com.lwe.core.domain.WorldMember;
import com.lwe.core.repository.WorldInviteRepository;
import com.lwe.core.repository.WorldMemberRepository;
import com.lwe.core.repository.WorldRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class WorldInviteService {

    private final WorldInviteRepository inviteRepo;
    private final WorldRepository worldRepo;
    private final WorldMemberRepository memberRepo;

    public WorldInviteService(WorldInviteRepository inviteRepo, WorldRepository worldRepo,
                              WorldMemberRepository memberRepo) {
        this.inviteRepo = inviteRepo;
        this.worldRepo = worldRepo;
        this.memberRepo = memberRepo;
    }

    @Transactional
    public WorldInvite create(UUID worldId, UUID userId, int maxUses, Instant expiresAt) {
        var world = worldRepo.findById(worldId)
            .orElseThrow(() -> new InviteException("WORLD_NOT_FOUND", "World not found"));
        if (!world.getOwnerId().equals(userId))
            throw new InviteException("WORLD_ACCESS_DENIED", "Only the owner can create invites");

        var token = HexFormat.of().formatHex(new SecureRandom().generateSeed(32));
        var invite = new WorldInvite(worldId, userId, token, maxUses);
        if (expiresAt != null) invite.setExpiresAt(expiresAt);
        return inviteRepo.save(invite);
    }

    public List<WorldInvite> listByWorld(UUID worldId, UUID userId) {
        requireOwner(worldId, userId);
        return inviteRepo.findByWorldIdOrderByCreatedAtDesc(worldId);
    }

    @Transactional
    public void deleteInvite(UUID inviteId, UUID userId) {
        var invite = inviteRepo.findById(inviteId)
            .orElseThrow(() -> new InviteException("INVITE_NOT_FOUND", "Invite not found"));
        requireOwner(invite.getWorldId(), userId);
        inviteRepo.delete(invite);
    }

    @Transactional
    public void join(String token, UUID userId) {
        var invite = inviteRepo.findByToken(token)
            .orElseThrow(() -> new InviteException("INVITE_NOT_FOUND", "Invalid invite token"));

        if (invite.getExpiresAt() != null && invite.getExpiresAt().isBefore(Instant.now()))
            throw new InviteException("INVITE_EXPIRED", "Invite token has expired");

        if (invite.getMaxUses() > 0 && invite.getUseCount() >= invite.getMaxUses())
            throw new InviteException("INVITE_EXHAUSTED", "Invite has already been used");

        if (memberRepo.existsByWorldIdAndUserId(invite.getWorldId(), userId))
            throw new InviteException("ALREADY_MEMBER", "You are already a member of this world");

        var member = new WorldMember(invite.getWorldId(), userId, "PLAYER");
        memberRepo.save(member);

        invite.setUseCount(invite.getUseCount() + 1);
        inviteRepo.save(invite);
    }

    private void requireOwner(UUID worldId, UUID userId) {
        var world = worldRepo.findById(worldId)
            .orElseThrow(() -> new InviteException("WORLD_NOT_FOUND", "World not found"));
        if (!world.getOwnerId().equals(userId))
            throw new InviteException("WORLD_ACCESS_DENIED", "Access denied");
    }

    public static class InviteException extends RuntimeException {
        private final String errorCode;
        public InviteException(String errorCode, String message) { super(message); this.errorCode = errorCode; }
        public String getErrorCode() { return errorCode; }
    }
}
