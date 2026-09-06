package com.lwe.core.service;

import com.lwe.core.domain.RefreshToken;
import com.lwe.core.domain.User;
import com.lwe.core.repository.RefreshTokenRepository;
import com.lwe.core.repository.UserRepository;
import com.lwe.security.JwtService;
import com.lwe.security.LoginRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepo;
    @Mock private RefreshTokenRepository refreshTokenRepo;
    @Mock private LoginRateLimiter rateLimiter;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final JwtService jwtService = new JwtService(
        "test-secret-key-which-must-be-at-least-32-characters-long-here", 24, 7
    );
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepo, refreshTokenRepo, passwordEncoder, jwtService, rateLimiter);
    }

    @Test
    void shouldRegisterUser() {
        when(userRepo.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepo.existsByUsername("hero")).thenReturn(false);
        when(userRepo.save(any())).thenAnswer(inv -> {
            var u = inv.<User>getArgument(0);
            setId(u, UUID.randomUUID());
            return u;
        });

        var result = authService.register("test@example.com", "hero", "password123!", "en");

        assertThat(result.accessToken()).isNotNull();
        assertThat(result.refreshToken()).isNotNull();
        assertThat(result.email()).isEqualTo("test@example.com");
        assertThat(result.username()).isEqualTo("hero");
        assertThat(result.locale()).isEqualTo("en");
        assertThat(result.role()).isEqualTo("USER");
    }

    @Test
    void shouldRejectDuplicateEmail() {
        when(userRepo.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() ->
            authService.register("existing@example.com", "hero", "pw", "de")
        ).isInstanceOf(AuthService.AuthException.class)
            .matches(e -> ((AuthService.AuthException) e).getErrorCode().equals("AUTH_EMAIL_TAKEN"));
    }

    @Test
    void shouldRejectDuplicateUsername() {
        when(userRepo.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepo.existsByUsername("taken")).thenReturn(true);

        assertThatThrownBy(() ->
            authService.register("new@example.com", "taken", "pw", "de")
        ).isInstanceOf(AuthService.AuthException.class)
            .matches(e -> ((AuthService.AuthException) e).getErrorCode().equals("AUTH_USERNAME_TAKEN"));
    }

    @Test
    void shouldLoginWithValidCredentials() {
        var user = userWithId("user@example.com", "player", passwordEncoder.encode("secret!"), "USER", "de");

        when(userRepo.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(rateLimiter.isBlocked("127.0.0.1")).thenReturn(false);

        var result = authService.login("user@example.com", "secret!", "127.0.0.1");

        assertThat(result.accessToken()).isNotNull();
        verify(rateLimiter).reset("127.0.0.1");
    }

    @Test
    void shouldRejectInvalidPassword() {
        var user = userWithId("user@example.com", "player", passwordEncoder.encode("correct!"), "USER", "de");

        when(userRepo.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(rateLimiter.isBlocked("127.0.0.1")).thenReturn(false);

        assertThatThrownBy(() ->
            authService.login("user@example.com", "wrong!", "127.0.0.1")
        ).isInstanceOf(AuthService.AuthException.class)
            .matches(e -> ((AuthService.AuthException) e).getErrorCode().equals("AUTH_INVALID_CREDENTIALS"));

        verify(rateLimiter).recordFailure("127.0.0.1");
    }

    @Test
    void shouldRejectWhenRateLimited() {
        when(rateLimiter.isBlocked("127.0.0.1")).thenReturn(true);

        assertThatThrownBy(() ->
            authService.login("t@t.com", "pw", "127.0.0.1")
        ).isInstanceOf(AuthService.AuthException.class)
            .matches(e -> ((AuthService.AuthException) e).getErrorCode().equals("AUTH_RATE_LIMITED"));
    }

    @Test
    void shouldRefreshToken() {
        var user = userWithId("user@example.com", "player", "hash", "USER", "de");
        var originalRefreshToken = jwtService.generateRefreshToken(user.getId().toString());
        var hash = sha256(originalRefreshToken);
        var stored = new RefreshToken(user.getId(), hash, java.time.Instant.now().plus(java.time.Duration.ofDays(7)));

        when(refreshTokenRepo.findByTokenHash(hash)).thenReturn(Optional.of(stored));
        when(userRepo.findById(user.getId())).thenReturn(Optional.of(user));

        var result = authService.refresh(originalRefreshToken);

        assertThat(result.accessToken()).isNotNull();
        assertThat(result.refreshToken()).isNotNull();
        assertThat(result.refreshToken()).isNotEqualTo(originalRefreshToken);
    }

    // -- helpers --

    private User userWithId(String email, String username, String passwordHash, String role, String locale) {
        var user = new User(email, username, passwordHash, role, locale);
        setId(user, UUID.randomUUID());
        return user;
    }

    private void setId(User user, UUID id) {
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String sha256(String value) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            var bytes = digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(bytes);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}