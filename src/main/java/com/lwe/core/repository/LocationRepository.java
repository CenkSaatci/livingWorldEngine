package com.lwe.core.repository;

import com.lwe.core.domain.Location;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LocationRepository extends JpaRepository<Location, UUID> {
    List<Location> findByRegionIdOrderByNameAsc(UUID regionId);
}
