package com.lwe.core.repository;

import com.lwe.core.domain.GameItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GameItemRepository extends JpaRepository<GameItem, UUID> {
    Optional<GameItem> findByIdAndWorldId(UUID id, UUID worldId);
}