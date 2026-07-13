package com.lwe.core.repository;

import com.lwe.core.domain.Quest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuestRepository extends JpaRepository<Quest, UUID> {
    List<Quest> findByWorldIdAndStatusOrderByCreatedAtDesc(UUID worldId, String status);
    List<Quest> findByWorldIdOrderByCreatedAtDesc(UUID worldId);
}
