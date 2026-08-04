package com.lwe.core.service;

import com.lwe.core.domain.Campaign;
import com.lwe.core.domain.CampaignMember;
import com.lwe.core.repository.CampaignMemberRepository;
import com.lwe.core.repository.CampaignRepository;
import com.lwe.core.repository.UserRepository;
import com.lwe.core.util.WorldAccess;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CampaignMemberService {

    private final CampaignMemberRepository memberRepo;
    private final CampaignRepository campaignRepo;
    private final UserRepository userRepo;
    private final WorldAccess worldAccess;

    public CampaignMemberService(CampaignMemberRepository memberRepo,
                                 CampaignRepository campaignRepo,
                                 UserRepository userRepo,
                                 WorldAccess worldAccess) {
        this.memberRepo = memberRepo;
        this.campaignRepo = campaignRepo;
        this.userRepo = userRepo;
        this.worldAccess = worldAccess;
    }

    /** Beim Erstellen der Kampagne wird der Ersteller automatisch DM. */
    @Transactional
    public CampaignMember addCreatorAsDm(Campaign campaign, UUID userId) {
        if (memberRepo.existsByCampaignIdAndUserId(campaign.getId(), userId)) {
            return memberRepo.findByCampaignIdAndUserId(campaign.getId(), userId).orElseThrow();
        }
        return memberRepo.save(new CampaignMember(campaign.getId(), userId, "DM"));
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
        return memberRepo.save(new CampaignMember(campaignId, targetUserId, role));
    }

    /** DM kann Spieler entfernen; der DM selbst kann nicht entfernt werden. */
    @Transactional
    public void removeMember(UUID campaignId, UUID actorUserId, UUID targetUserId) {
        var campaign = requireCampaign(campaignId);
        worldAccess.requireAccess(campaign.getWorldId(), actorUserId);
        var member = memberRepo.findByCampaignIdAndUserId(campaignId, targetUserId)
            .orElseThrow(() -> new CampaignMemberException("MEMBER_NOT_FOUND", "Member not found"));
        if (member.getRole().equals("DM")) {
            throw new CampaignMemberException("DM_REMOVAL_DENIED", "The DM cannot be removed");
        }
        requireDm(campaignId, actorUserId);
        memberRepo.delete(member);
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
