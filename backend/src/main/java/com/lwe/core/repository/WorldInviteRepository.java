package com.lwe.core.repository;

import com.lwe.core.domain.WorldInvite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorldInviteRepository extends JpaRepository<WorldInvite, UUID> {
    Optional<WorldInvite> findByToken(String token);
    List<WorldInvite> findByWorldIdOrderByCreatedAtDesc(UUID worldId);
    void deleteByWorldId(UUID worldId);
}
