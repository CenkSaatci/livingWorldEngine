package com.lwe.core.repository;

import com.lwe.core.domain.Trade;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TradeRepository extends JpaRepository<Trade, UUID> {

    /** Runde 1 (F6): Row-Lock gegen Doppel-Accept. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Trade t WHERE t.id = :id")
    java.util.Optional<Trade> findByIdForUpdate(@Param("id") UUID id);

    @Query("SELECT t FROM Trade t WHERE t.worldId = :worldId "
        + "AND (t.proposerEntityId = :entityId OR t.partnerEntityId = :entityId) "
        + "ORDER BY t.updatedAt DESC")
    List<Trade> findByWorldAndEntity(@Param("worldId") UUID worldId,
                                     @Param("entityId") UUID entityId);
}
