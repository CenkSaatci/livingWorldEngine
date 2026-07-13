package com.lwe.core.repository;

import com.lwe.core.domain.WorldEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorldEventRepository extends JpaRepository<WorldEvent, Long> {
    List<WorldEvent> findByWorldIdAndIdGreaterThanOrderByIdAsc(UUID worldId, Long sinceId);
}