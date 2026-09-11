package com.lwe.core.repository;

import com.lwe.core.domain.NpcIntent;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NpcIntentRepository extends JpaRepository<NpcIntent, UUID> {
    List<NpcIntent> findByWorldIdAndStatusOrderByCreatedAtDesc(UUID worldId, String status);

    /** F3-Audit: serialisiert Statuswechsel (approve/reject). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM NpcIntent i WHERE i.id = :id")
    Optional<NpcIntent> findByIdForUpdate(@Param("id") UUID id);
    List<NpcIntent> findByWorldIdAndStatusAndIntentTypeOrderByCreatedAtDesc(
        UUID worldId, String status, String intentType);
    List<NpcIntent> findByWorldIdOrderByCreatedAtDesc(UUID worldId);
}
