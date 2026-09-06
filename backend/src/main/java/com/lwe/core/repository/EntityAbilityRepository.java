package com.lwe.core.repository;

import com.lwe.core.domain.EntityAbility;
import com.lwe.core.domain.EntityAbilityId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EntityAbilityRepository extends JpaRepository<EntityAbility, EntityAbilityId> {
    List<EntityAbility> findByEntityId(UUID entityId);
    Optional<EntityAbility> findByEntityIdAndAbilityId(UUID entityId, UUID abilityId);
}
