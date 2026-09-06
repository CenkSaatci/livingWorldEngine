package com.lwe.core.repository;

import com.lwe.core.domain.GameSystem;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameSystemRepository extends JpaRepository<GameSystem, UUID> {

    @Cacheable("gameSystems")
    Optional<GameSystem> findById(UUID id);

    @CacheEvict(value = "gameSystems", key = "#p0.id")
    <S extends GameSystem> S save(S entity);

    List<GameSystem> findByActiveTrue();
    boolean existsByName(String name);
}