package com.lwe.core.repository;

import com.lwe.core.domain.Adventure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AdventureRepository extends JpaRepository<Adventure, UUID> {
    List<Adventure> findByWorldId(UUID worldId);
}