package com.lwe.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Zentrale JWT-Utility für Access- und Refresh-Tokens.
 *
 * <p>Access-Token: 24 h gültig, enthält userId + role.
 * Refresh-Token: 7 d gültig, enthält userId (nicht zum Autorisieren verwendet).
 *
 * @see <a href="../../../docs/ADR/001-frontend-react-vite.md">ADR-001: Token in memory</a>
 */
public class JwtService {

    private final SecretKey signingKey;
    private final long accessTokenExpirationHours;
    private final long refreshTokenExpirationDays;

    public JwtService(String secret, long accessTokenExpirationHours, long refreshTokenExpirationDays) {
        if (secret.length() < 32) {
            throw new IllegalArgumentException("JWT-Secret muss mindestens 32 Zeichen lang sein");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationHours = accessTokenExpirationHours;
        this.refreshTokenExpirationDays = refreshTokenExpirationDays;
    }

    public String generateAccessToken(String userId, String role) {
        return Jwts.builder()
            .id(UUID.randomUUID().toString())
            .subject(userId)
            .claim("role", role)
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plus(Duration.ofHours(accessTokenExpirationHours))))
            .signWith(signingKey)
            .compact();
    }

    public String generateRefreshToken(String userId) {
        return Jwts.builder()
            .id(UUID.randomUUID().toString())
            .subject(userId)
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plus(Duration.ofDays(refreshTokenExpirationDays))))
            .signWith(signingKey)
            .compact();
    }

    public String extractUserId(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (ExpiredJwtException e) {
            throw new TokenExpiredException("Token ist abgelaufen", e);
        } catch (SecurityException | MalformedJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            throw new TokenInvalidException("Token ist ungültig", e);
        }
    }

    public static class TokenExpiredException extends RuntimeException {
        public TokenExpiredException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class TokenInvalidException extends RuntimeException {
        public TokenInvalidException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}