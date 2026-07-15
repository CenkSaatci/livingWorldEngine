package com.lwe.core.repository;

import com.lwe.core.domain.World;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorldRepository extends JpaRepository<World, UUID> {
    List<World> findByOwnerIdAndActiveTrue(UUID ownerId);
    List<World> findByActiveTrue();
    long countByOwnerIdAndActiveTrue(UUID ownerId);
}