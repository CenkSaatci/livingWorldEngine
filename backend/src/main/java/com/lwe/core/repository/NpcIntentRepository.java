package com.lwe.core.repository;

import com.lwe.core.domain.NpcIntent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NpcIntentRepository extends JpaRepository<NpcIntent, UUID> {
    List<NpcIntent> findByWorldIdAndStatusOrderByCreatedAtDesc(UUID worldId, String status);
    List<NpcIntent> findByWorldIdOrderByCreatedAtDesc(UUID worldId);
}
