package com.lwe.core.repository;

import com.lwe.core.domain.CombatSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CombatSessionRepository extends JpaRepository<CombatSession, UUID> {

    Optional<CombatSession> findFirstByWorldIdAndStatusOrderByCreatedAtDesc(UUID worldId, String status);
}