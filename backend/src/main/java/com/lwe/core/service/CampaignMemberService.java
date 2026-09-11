package com.lwe.core.service;

import com.lwe.core.domain.Campaign;
import com.lwe.core.domain.CampaignMember;
import com.lwe.core.domain.WorldMember;
import com.lwe.core.repository.CampaignMemberRepository;
import com.lwe.core.repository.CampaignRepository;
import com.lwe.core.repository.UserRepository;
import com.lwe.core.repository.WorldMemberRepository;
import com.lwe.core.repository.WorldRepository;
import com.lwe.core.util.WorldAccess;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class CampaignMemberService {

    private final CampaignMemberRepository memberRepo;
    private final CampaignRepository campaignRepo;
    private final UserRepository userRepo;
    private final WorldAccess worldAccess;
    private final WorldMemberRepository worldMemberRepo;
    private final WorldRepository worldRepo;

    public CampaignMemberService(CampaignMemberRepository memberRepo,
                                 CampaignRepository campaignRepo,
                                 UserRepository userRepo,
                                 WorldAccess worldAccess,
                                 WorldMemberRepository worldMemberRepo,
                                 WorldRepository worldRepo) {
        this.memberRepo = memberRepo;
        this.campaignRepo = campaignRepo;
        this.userRepo = userRepo;
        this.worldAccess = worldAccess;
        this.worldMemberRepo = worldMemberRepo;
        this.worldRepo = worldRepo;
    }

    /** P27-T03: Campaign-Rollen auf World-Members spiegeln, damit Spieler Weltzugriff haben. */
    private void syncWorldMember(Campaign campaign, UUID userId, String role) {
        var world = worldRepo.findById(campaign.getWorldId()).orElse(null);
        if (world != null && world.getOwnerId().equals(userId)) return; // Owner ist implizit DM
        var existing = worldMemberRepo.findByWorldIdAndUserId(campaign.getWorldId(), userId);
        if (existing.isPresent()) {
            existing.get().setRole(role);
            worldMemberRepo.save(existing.get());
        } else {
            worldMemberRepo.save(new WorldMember(campaign.getWorldId(), userId, role));
        }
    }

    private void removeWorldMember(Campaign campaign, UUID userId) {
        worldMemberRepo.findByWorldIdAndUserId(campaign.getWorldId(), userId)
            .ifPresent(worldMemberRepo::delete);
    }

    /** Beim Erstellen der Kampagne wird der Ersteller automatisch DM. */
    @Transactional
    public CampaignMember addCreatorAsDm(Campaign campaign, UUID userId) {
        if (memberRepo.existsByCampaignIdAndUserId(campaign.getId(), userId)) {
            return memberRepo.findByCampaignIdAndUserId(campaign.getId(), userId).orElseThrow();
        }
        var member = memberRepo.save(new CampaignMember(campaign.getId(), userId, "DM"));
        syncWorldMember(campaign, userId, "DM");
        return member;
    }

    private static final Set<String> ROLES = Set.of("PLAYER", "DM");

    private void validateRole(String role) {
        if (role == null || !ROLES.contains(role)) {
            throw new CampaignMemberException("INVALID_ROLE", "role must be PLAYER or DM");
        }
    }

    /** Nur der DM (bzw. Welt-Owner) kann Mitglieder hinzufügen. */
    @Transactional
    public CampaignMember addMember(UUID campaignId, UUID actorUserId, UUID targetUserId, String role) {
        var campaign = requireCampaign(campaignId);
        worldAccess.requireAccess(campaign.getWorldId(), actorUserId);
        var user = userRepo.findById(targetUserId)
            .orElseThrow(() -> new CampaignMemberException("USER_NOT_FOUND", "User not found"));
        if (memberRepo.existsByCampaignIdAndUserId(campaignId, targetUserId)) {
            throw new CampaignMemberException("MEMBER_ALREADY", "User is already a campaign member");
        }
        requireDm(campaignId, actorUserId);
        validateRole(role);
        var saved = memberRepo.save(new CampaignMember(campaignId, targetUserId, role));
        syncWorldMember(campaign, targetUserId, role);
        return saved;
    }

    /** DM kann Mitglieder entfernen; der letzte DM bleibt geschützt (P27-T02). */
    @Transactional
    public void removeMember(UUID campaignId, UUID actorUserId, UUID targetUserId) {
        var campaign = requireCampaign(campaignId);
        worldAccess.requireAccess(campaign.getWorldId(), actorUserId);
        var member = memberRepo.findByCampaignIdAndUserId(campaignId, targetUserId)
            .orElseThrow(() -> new CampaignMemberException("MEMBER_NOT_FOUND", "Member not found"));
        requireDm(campaignId, actorUserId);
        if (member.getRole().equals("DM") && memberRepo.countByCampaignIdAndRole(campaignId, "DM") <= 1) {
            throw new CampaignMemberException("DM_REMOVAL_DENIED", "The last DM cannot be removed");
        }
        memberRepo.delete(member);
        removeWorldMember(campaign, targetUserId);
    }

    /** Rolle aendern (P27-T02): DM kann befoerdern/degradieren; letzter DM geschuetzt. */
    @Transactional
    public CampaignMember updateRole(UUID campaignId, UUID actorUserId, UUID targetUserId, String role) {
        var campaign = requireCampaign(campaignId);
        worldAccess.requireAccess(campaign.getWorldId(), actorUserId);
        requireDm(campaignId, actorUserId);
        validateRole(role);
        var member = memberRepo.findByCampaignIdAndUserId(campaignId, targetUserId)
            .orElseThrow(() -> new CampaignMemberException("MEMBER_NOT_FOUND", "Member not found"));
        if (member.getRole().equals("DM") && role.equals("PLAYER")
            && memberRepo.countByCampaignIdAndRole(campaignId, "DM") <= 1) {
            throw new CampaignMemberException("LAST_DM", "At least one DM must remain");
        }
        member.setRole(role);
        var saved = memberRepo.save(member);
        syncWorldMember(campaign, targetUserId, role);
        return saved;
    }

    public List<CampaignMember> listMembers(UUID campaignId, UUID actorUserId) {
        var campaign = requireCampaign(campaignId);
        worldAccess.requireAccess(campaign.getWorldId(), actorUserId);
        return memberRepo.findByCampaignId(campaignId);
    }

    public boolean isDm(UUID campaignId, UUID userId) {
        return memberRepo.findByCampaignIdAndUserId(campaignId, userId)
            .map(m -> m.getRole().equals("DM"))
            .orElse(false);
    }

    public boolean isMember(UUID campaignId, UUID userId) {
        return memberRepo.existsByCampaignIdAndUserId(campaignId, userId);
    }

    private Campaign requireCampaign(UUID campaignId) {
        return campaignRepo.findById(campaignId)
            .orElseThrow(() -> new CampaignMemberException("CAMPAIGN_NOT_FOUND", "Campaign not found"));
    }

    private void requireDm(UUID campaignId, UUID userId) {
        if (!isDm(campaignId, userId)) {
            throw new CampaignMemberException("DM_REQUIRED", "Only the DM may manage members");
        }
    }

    public static class CampaignMemberException extends RuntimeException {
        private final String errorCode;
        public CampaignMemberException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }
        public String getErrorCode() { return errorCode; }
    }
}
