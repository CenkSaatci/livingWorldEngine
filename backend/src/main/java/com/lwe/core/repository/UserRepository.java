package com.lwe.core.repository;

import com.lwe.core.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    Optional<User> findByVerificationToken(String token);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    List<User> findByEmailContainingIgnoreCaseOrUsernameContainingIgnoreCase(String email, String username);
}