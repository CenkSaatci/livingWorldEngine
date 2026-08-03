package com.lwe.core.repository;

import com.lwe.core.domain.Ability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AbilityRepository extends JpaRepository<Ability, UUID> {
    List<Ability> findByGameSystemIdOrderByNameAsc(UUID gameSystemId);
}
