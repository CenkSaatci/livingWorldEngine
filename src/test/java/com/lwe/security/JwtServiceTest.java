package com.lwe.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 🔴 RED: JwtService existiert noch nicht → Kompilerfehler.
 * 🟢 GREEN: Implementierung erzeugen.
 */
class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(
            "test-secret-key-which-must-be-at-least-32-characters-long-here",
            24,
            7
        );
    }

    @Test
    void shouldGenerateAccessToken() {
        var token = jwtService.generateAccessToken("user-123", "USER");
        assertThat(token).isNotNull();
        assertThat(token).isInstanceOf(String.class);
    }

    @Test
    void shouldValidateAccessToken() {
        var token = jwtService.generateAccessToken("user-123", "USER");
        var userId = jwtService.extractUserId(token);
        assertThat(userId).isEqualTo("user-123");
    }

    @Test
    void shouldExtractRoleFromToken() {
        var token = jwtService.generateAccessToken("user-123", "ADMIN");
        var role = jwtService.extractRole(token);
        assertThat(role).isEqualTo("ADMIN");
    }

    @Test
    void shouldRejectExpiredToken() {
        // Token mit sehr kurzer Gültigkeit
        var shortLived = new JwtService("test-secret-key-which-must-be-at-least-32-characters-long-here", 0, 7);
        var token = shortLived.generateAccessToken("user-123", "USER");

        assertThatThrownBy(() -> shortLived.extractUserId(token))
            .isInstanceOf(JwtService.TokenExpiredException.class);
    }

    @Test
    void shouldRejectInvalidSignature() {
        var token = jwtService.generateAccessToken("user-123", "USER");
        var fakeService = new JwtService("different-secret-key-which-must-be-at-least-32-characters-long!", 24, 7);

        assertThatThrownBy(() -> fakeService.extractUserId(token))
            .isInstanceOf(JwtService.TokenInvalidException.class);
    }

    @Test
    void shouldGenerateRefreshToken() {
        var token = jwtService.generateRefreshToken("user-123");
        assertThat(token).isNotNull();
        var userId = jwtService.extractUserId(token);
        assertThat(userId).isEqualTo("user-123");
    }
}