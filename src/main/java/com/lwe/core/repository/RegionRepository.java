package com.lwe.core.repository;

import com.lwe.core.domain.Region;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RegionRepository extends JpaRepository<Region, UUID> {
    List<Region> findByWorldIdOrderByNameAsc(UUID worldId);
}
