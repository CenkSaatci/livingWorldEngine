package com.lwe.core.repository;

import com.lwe.core.domain.GameEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface GameEntityRepository extends JpaRepository<GameEntity, UUID> {
    List<GameEntity> findByWorldIdAndActiveTrue(UUID worldId);
    List<GameEntity> findByWorldIdAndEntityTypeAndActiveTrue(UUID worldId, String entityType);
    long countByWorldIdAndActiveTrue(UUID worldId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM GameEntity e WHERE e.id = :id")
    java.util.Optional<GameEntity> findByIdForUpdate(@Param("id") UUID id);

    @Query(value = "SELECT * FROM entities WHERE metadata_json @> CAST(:jsonFilter AS jsonb) AND active = true", nativeQuery = true)
    List<GameEntity> findByMetadataJsonFilter(@Param("jsonFilter") String jsonFilter);
}