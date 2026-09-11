package com.lwe.core.repository;

import com.lwe.core.domain.CampaignMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CampaignMemberRepository extends JpaRepository<CampaignMember, UUID> {
    boolean existsByCampaignIdAndUserId(UUID campaignId, UUID userId);
    Optional<CampaignMember> findByCampaignIdAndUserId(UUID campaignId, UUID userId);
    List<CampaignMember> findByCampaignId(UUID campaignId);
    long countByCampaignId(UUID campaignId);
    long countByCampaignIdAndRole(UUID campaignId, String role);
    boolean existsByCampaignIdAndUserIdAndRole(UUID campaignId, UUID userId, String role);
}
