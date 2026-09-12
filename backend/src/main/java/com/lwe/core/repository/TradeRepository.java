package com.lwe.core.repository;

import com.lwe.core.domain.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TradeRepository extends JpaRepository<Trade, UUID> {

    @Query("SELECT t FROM Trade t WHERE t.worldId = :worldId "
        + "AND (t.proposerEntityId = :entityId OR t.partnerEntityId = :entityId) "
        + "ORDER BY t.updatedAt DESC")
    List<Trade> findByWorldAndEntity(@Param("worldId") UUID worldId,
                                     @Param("entityId") UUID entityId);
}
