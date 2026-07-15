package com.lwe.core.repository;

import com.lwe.core.domain.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenAndUsedFalse(String token);
    void deleteByUserId(UUID userId);
}
