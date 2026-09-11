package com.lwe.core.repository;

import com.lwe.core.domain.World;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface WorldRepository extends JpaRepository<World, UUID> {
    List<World> findByOwnerIdAndActiveTrue(UUID ownerId);
    List<World> findByActiveTrue();
    long countByOwnerIdAndActiveTrue(UUID ownerId);

    /** Wie countByOwnerIdAndActiveTrue, aber ohne Kampagnen-Fork-Welten (P27-T03/Audit). */
    @Query("SELECT COUNT(w) FROM World w WHERE w.ownerId = :ownerId AND w.active = true "
        + "AND NOT EXISTS (SELECT c FROM Campaign c WHERE c.worldId = w.id AND c.forkedWorld = true)")
    long countQuotaRelevantByOwnerId(@Param("ownerId") UUID ownerId);

    @Query(value = """
        (SELECT * FROM worlds WHERE owner_id = :userId AND active = true)
        UNION
        (SELECT w.* FROM worlds w JOIN world_members m ON w.id = m.world_id
         WHERE m.user_id = :userId AND w.active = true AND w.owner_id != :userId
           AND w.visibility != 'PRIVATE')
        UNION
        (SELECT * FROM worlds WHERE visibility = 'PUBLIC' AND active = true)
        ORDER BY created_at DESC
        """,
        countQuery = """
        SELECT COUNT(*) FROM (
          (SELECT id FROM worlds WHERE owner_id = :userId AND active = true)
          UNION
          (SELECT w.id FROM worlds w JOIN world_members m ON w.id = m.world_id
           WHERE m.user_id = :userId AND w.active = true AND w.owner_id != :userId
             AND w.visibility != 'PRIVATE')
          UNION
          (SELECT id FROM worlds WHERE visibility = 'PUBLIC' AND active = true)
        ) AS cnt
        """,
        nativeQuery = true)
    Page<World> findAccessibleByUserId(@Param("userId") UUID userId, Pageable pageable);
}