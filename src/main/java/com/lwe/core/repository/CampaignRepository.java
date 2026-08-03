package com.lwe.core.repository;

import com.lwe.core.domain.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CampaignRepository extends JpaRepository<Campaign, UUID> {
    List<Campaign> findByWorldId(UUID worldId);
    List<Campaign> findByGameSystemId(UUID gameSystemId);
    List<Campaign> findByWorldIdIn(List<UUID> worldIds);
}
