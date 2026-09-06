package com.lwe.core.repository;

import com.lwe.core.domain.EntityMemory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface EntityMemoryRepository extends JpaRepository<EntityMemory, Long> {
    List<EntityMemory> findByEntityIdOrderByCreatedAtDesc(UUID entityId);
    List<EntityMemory> findBySubjectIdOrderByCreatedAtDesc(UUID subjectId);
    List<EntityMemory> findByCreatedAtBefore(Instant cutoff);
    List<EntityMemory> findByCreatedAtBeforeAndSentiment(Instant cutoff, int sentiment);
}
