package com.lwe.core.repository;

import com.lwe.core.domain.AdventureProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AdventureProgressRepository extends JpaRepository<AdventureProgress, UUID> {
    Optional<AdventureProgress> findByAdventureIdAndEntityId(UUID adventureId, UUID entityId);
    java.util.List<AdventureProgress> findByAdventureIdAndStatus(UUID adventureId, String status);

    java.util.List<AdventureProgress> findByAdventureId(UUID adventureId);
}