package com.lwe.core.repository;

import com.lwe.core.domain.Adventure;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdventureRepository extends JpaRepository<Adventure, UUID> {
    List<Adventure> findByWorldId(UUID worldId);

    /** QA-Audit: Start serialisieren (Race beim doppelten Auto-Start vermeiden). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Adventure a where a.id = :id")
    Optional<Adventure> findByIdForUpdate(@Param("id") UUID id);
    List<Adventure> findByLocationId(UUID locationId);
    List<Adventure> findByGiverEntityId(UUID giverEntityId);
}
