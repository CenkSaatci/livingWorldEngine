package com.lwe.core.repository;

import com.lwe.core.domain.EntityRelationship;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EntityRelationshipRepository extends JpaRepository<EntityRelationship, Long> {
    List<EntityRelationship> findByEntityAIdOrEntityBId(UUID entityAId, UUID entityBId);
    Optional<EntityRelationship> findByEntityAIdAndEntityBId(UUID a, UUID b);
}
