package com.lwe.core.repository;

import com.lwe.core.domain.WorldMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorldMemberRepository extends JpaRepository<WorldMember, UUID> {
    boolean existsByWorldIdAndUserId(UUID worldId, UUID userId);
    Optional<WorldMember> findByWorldIdAndUserId(UUID worldId, UUID userId);
    List<WorldMember> findByWorldId(UUID worldId);
    long countByWorldId(UUID worldId);

    @Query("SELECT wm.worldId FROM WorldMember wm WHERE wm.userId = :userId")
    List<UUID> findWorldIdsByUserId(UUID userId);
}