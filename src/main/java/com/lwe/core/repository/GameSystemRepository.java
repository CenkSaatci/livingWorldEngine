package com.lwe.core.repository;

import com.lwe.core.domain.GameSystem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GameSystemRepository extends JpaRepository<GameSystem, UUID> {
    List<GameSystem> findByActiveTrue();
    boolean existsByName(String name);
}