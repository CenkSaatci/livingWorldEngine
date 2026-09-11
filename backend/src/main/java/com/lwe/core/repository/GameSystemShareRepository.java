package com.lwe.core.repository;

import com.lwe.core.domain.GameSystemShare;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameSystemShareRepository extends JpaRepository<GameSystemShare, UUID> {
    List<GameSystemShare> findBySystemId(UUID systemId);
    Optional<GameSystemShare> findBySystemIdAndUserId(UUID systemId, UUID userId);
    boolean existsBySystemIdAndUserId(UUID systemId, UUID userId);
    void deleteBySystemIdAndUserId(UUID systemId, UUID userId);
}
