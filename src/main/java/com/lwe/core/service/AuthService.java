package com.lwe.core.service;

import com.lwe.core.domain.RefreshToken;
import com.lwe.core.domain.User;
import com.lwe.core.repository.RefreshTokenRepository;
import com.lwe.core.repository.UserRepository;
import com.lwe.security.JwtService;
import com.lwe.security.LoginRateLimiter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepo;
    private final RefreshTokenRepository refreshTokenRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginRateLimiter rateLimiter;

    public AuthService(UserRepository userRepo,
                       RefreshTokenRepository refreshTokenRepo,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       LoginRateLimiter rateLimiter) {
        this.userRepo = userRepo;
        this.refreshTokenRepo = refreshTokenRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.rateLimiter = rateLimiter;
    }

    @Transactional
    public AuthResult register(String email, String username, String password, String locale) {
        if (userRepo.existsByEmail(email)) {
            throw new AuthException("AUTH_EMAIL_TAKEN", "Email already registered");
        }
        if (userRepo.existsByUsername(username)) {
            throw new AuthException("AUTH_USERNAME_TAKEN", "Username already taken");
        }

        var hash = passwordEncoder.encode(password);
        var user = new User(email, username, hash, "USER", locale != null ? locale : "de");

        user.setEmailVerifiedAt(Instant.now());

        user = userRepo.save(user);

        return createAuthResult(user);
    }

    @Transactional
    public AuthResult login(String email, String password, String ip) {
        if (rateLimiter.isBlocked(ip)) {
            throw new AuthException("AUTH_RATE_LIMITED", "Too many login attempts");
        }

        var user = userRepo.findByEmail(email)
            .orElseThrow(() -> {
                rateLimiter.recordFailure(ip);
                return new AuthException("AUTH_INVALID_CREDENTIALS", "Invalid email or password");
            });

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            rateLimiter.recordFailure(ip);
            throw new AuthException("AUTH_INVALID_CREDENTIALS", "Invalid email or password");
        }

        rateLimiter.reset(ip);
        return createAuthResult(user);
    }

    @Transactional
    public AuthResult refresh(String rawToken) {
        UUID userId;
        try {
            userId = UUID.fromString(jwtService.extractUserId(rawToken));
        } catch (JwtService.TokenExpiredException | JwtService.TokenInvalidException e) {
            throw new AuthException("AUTH_REFRESH_INVALID", "Refresh token is invalid or expired");
        }

        var hash = sha256(rawToken);
        var stored = refreshTokenRepo.findByTokenHash(hash)
            .orElseThrow(() -> new AuthException("AUTH_REFRESH_INVALID", "Refresh token not found"));

        if (stored.isRevoked()) {
            // Möglicher Token-Diebstahl — widerrufe alle Tokens für diesen User
            refreshTokenRepo.deleteByUserId(stored.getUserId());
            throw new AuthException("AUTH_REFRESH_INVALID", "Refresh token has been revoked");
        }

        if (stored.getExpiresAt().isBefore(Instant.now())) {
            refreshTokenRepo.delete(stored);
            throw new AuthException("AUTH_REFRESH_EXPIRED", "Refresh token has expired");
        }

        stored.setRevoked(true);
        refreshTokenRepo.save(stored);

        var user = userRepo.findById(userId)
            .orElseThrow(() -> new AuthException("USER_NOT_FOUND", "User not found"));

        return createAuthResult(user);
    }

    @Transactional
    public AuthResult serviceLogin(String serviceUser, String servicePassword) {
        // Spezifischer Service-Account für den AI-Bot
        var user = userRepo.findByUsername(serviceUser)
            .orElseThrow(() -> new AuthException("AUTH_INVALID_CREDENTIALS", "Invalid service credentials"));

        if (!passwordEncoder.matches(servicePassword, user.getPasswordHash())) {
            throw new AuthException("AUTH_INVALID_CREDENTIALS", "Invalid service credentials");
        }

        return createAuthResult(user);
    }

    private AuthResult createAuthResult(User user) {
        var accessToken = jwtService.generateAccessToken(
            user.getId().toString(), user.getRole()
        );
        var rawRefresh = jwtService.generateRefreshToken(user.getId().toString());

        persistRefreshToken(user.getId(), rawRefresh);

        return new AuthResult(
            accessToken,
            rawRefresh,
            Duration.ofHours(24).toSeconds(),
            user.getId(),
            user.getEmail(),
            user.getUsername(),
            user.getRole(),
            user.getLocale(),
            user.getVerificationToken(),
            user.getEmailVerifiedAt() != null
        );
    }

    @Transactional
    public User verifyEmail(String token) {
        var user = userRepo.findByVerificationToken(token)
            .orElseThrow(() -> new AuthException("AUTH_VERIFICATION_INVALID", "Invalid verification token"));
        if (user.getVerificationTokenExpiresAt() != null
            && user.getVerificationTokenExpiresAt().isBefore(Instant.now())) {
            throw new AuthException("AUTH_VERIFICATION_EXPIRED", "Verification token expired");
        }
        user.setEmailVerifiedAt(Instant.now());
        user.setVerificationToken(null);
        user.setVerificationTokenExpiresAt(null);
        return userRepo.save(user);
    }

    @Transactional
    public String resendVerification(String email) {
        var user = userRepo.findByEmail(email)
            .orElseThrow(() -> new AuthException("AUTH_USER_NOT_FOUND", "User not found"));
        if (user.getEmailVerifiedAt() != null) {
            throw new AuthException("AUTH_ALREADY_VERIFIED", "Email already verified");
        }
        var token = HexFormat.of().formatHex(new SecureRandom().generateSeed(32));
        user.setVerificationToken(token);
        user.setVerificationTokenExpiresAt(Instant.now().plus(Duration.ofHours(24)));
        userRepo.save(user);
        return token;
    }

    private void persistRefreshToken(UUID userId, String rawToken) {
        var hash = sha256(rawToken);
        var expiresAt = Instant.now().plus(Duration.ofDays(7));
        var entity = new RefreshToken(userId, hash, expiresAt);
        refreshTokenRepo.save(entity);
    }

    private String sha256(String value) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public record AuthResult(
        String accessToken,
        String refreshToken,
        long expiresIn,
        UUID id,
        String email,
        String username,
        String role,
        String locale,
        String verificationToken,
        boolean emailVerified
    ) {}

    public static class AuthException extends RuntimeException {
        private final String errorCode;
        public AuthException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }
        public String getErrorCode() { return errorCode; }
    }
}