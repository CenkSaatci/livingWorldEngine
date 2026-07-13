package com.lwe.core.repository;

import com.lwe.core.domain.AdventureNode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AdventureNodeRepository extends JpaRepository<AdventureNode, UUID> {
    List<AdventureNode> findByAdventureId(UUID adventureId);
}