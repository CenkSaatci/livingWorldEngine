package com.lwe.core.repository;

import com.lwe.core.domain.GameSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GameSessionRepository extends JpaRepository<GameSession, UUID> {
    List<GameSession> findByWorldIdAndStatusOrderByStartedAtDesc(UUID worldId, String status);
}
