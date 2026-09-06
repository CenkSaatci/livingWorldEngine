package com.lwe.core.repository;

import com.lwe.core.domain.Faction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FactionRepository extends JpaRepository<Faction, UUID> {
    List<Faction> findByWorldIdOrderByNameAsc(UUID worldId);
}
