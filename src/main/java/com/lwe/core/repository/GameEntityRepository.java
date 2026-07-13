package com.lwe.core.repository;

import com.lwe.core.domain.GameEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface GameEntityRepository extends JpaRepository<GameEntity, UUID> {
    List<GameEntity> findByWorldIdAndActiveTrue(UUID worldId);
    List<GameEntity> findByWorldIdAndEntityTypeAndActiveTrue(UUID worldId, String entityType);
    long countByWorldIdAndActiveTrue(UUID worldId);

    @Query(value = "SELECT * FROM entities WHERE metadata_json @> :jsonFilter AND active = true", nativeQuery = true)
    List<GameEntity> findByMetadataJsonFilter(@Param("jsonFilter") String jsonFilter);
}