package com.lwe.core.repository;

import com.lwe.core.domain.NodeChoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NodeChoiceRepository extends JpaRepository<NodeChoice, UUID> {
    List<NodeChoice> findByNodeId(UUID nodeId);
}