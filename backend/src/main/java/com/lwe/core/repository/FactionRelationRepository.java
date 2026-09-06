package com.lwe.core.repository;

import com.lwe.core.domain.FactionRelation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FactionRelationRepository extends JpaRepository<FactionRelation, UUID> {
    List<FactionRelation> findByFactionAIdOrFactionBId(UUID factionAId, UUID factionBId);
    Optional<FactionRelation> findByFactionAIdAndFactionBId(UUID a, UUID b);
    List<FactionRelation> findByFactionAIdIn(List<UUID> factionIds);
}
