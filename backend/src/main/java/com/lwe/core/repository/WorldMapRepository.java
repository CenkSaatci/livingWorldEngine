package com.lwe.core.repository;

import com.lwe.core.domain.WorldMap;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WorldMapRepository extends JpaRepository<WorldMap, UUID> {
    Optional<WorldMap> findByWorldId(UUID worldId);
}
