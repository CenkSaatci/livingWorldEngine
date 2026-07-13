package com.lwe.core.repository;

import com.lwe.core.domain.AdventureProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AdventureProgressRepository extends JpaRepository<AdventureProgress, UUID> {
    Optional<AdventureProgress> findByAdventureIdAndEntityId(UUID adventureId, UUID entityId);
}