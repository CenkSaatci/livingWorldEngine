package com.lwe.core.repository;

import com.lwe.core.domain.EntityEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EntityEventRepository extends JpaRepository<EntityEvent, Long> {
    List<EntityEvent> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, UUID entityId);
}
