package com.lwe.core.repository;

import com.lwe.core.domain.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CampaignRepository extends JpaRepository<Campaign, UUID> {
    List<Campaign> findByWorldId(UUID worldId);
    List<Campaign> findByGameSystemId(UUID gameSystemId);

    /** Serialisiert Rollen-/Entfernen-Checks (letzter DM, Audit P27). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Campaign c WHERE c.id = :id")
    java.util.Optional<Campaign> findByIdForUpdate(@Param("id") UUID id);
    List<Campaign> findByWorldIdIn(List<UUID> worldIds);
}
