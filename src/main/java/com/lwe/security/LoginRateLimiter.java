package com.lwe.security;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Einfacher In-Memory-Rate-Limiter für Login-Endpunkte.
 *
 * <p>Begrenzt Fehlversuche pro IP auf {@code maxAttempts} innerhalb eines {@code windowDuration}.
 * Nach Erreichen des Limits wird die IP für die Dauer des Fensters blockiert.
 */
@Component
public class LoginRateLimiter {

    private final int maxAttempts;
    private final Duration windowDuration;
    private final Map<String, AttemptWindow> attempts = new ConcurrentHashMap<>();

    public LoginRateLimiter() {
        this(5, Duration.ofMinutes(1));
    }

    public LoginRateLimiter(int maxAttempts, Duration windowDuration) {
        this.maxAttempts = maxAttempts;
        this.windowDuration = windowDuration;
    }

    /**
     * Prüft, ob eine IP blockiert ist (zu viele Fehlversuche).
     */
    public boolean isBlocked(String ip) {
        var window = attempts.get(ip);
        if (window == null) return false;
        if (window.expiresAt.isBefore(Instant.now())) {
            attempts.remove(ip);
            return false;
        }
        return window.count >= maxAttempts;
    }

    /**
     * Registriert einen Fehlversuch für die gegebene IP.
     */
    public void recordFailure(String ip) {
        var now = Instant.now();
        attempts.compute(ip, (key, window) -> {
            if (window == null || window.expiresAt.isBefore(now)) {
                return new AttemptWindow(1, now.plus(windowDuration));
            }
            window.count++;
            return window;
        });
    }

    /**
     * Setzt den Zähler für eine IP zurück (nach erfolgreichem Login).
     */
    public void reset(String ip) {
        attempts.remove(ip);
    }

    private static class AttemptWindow {
        int count;
        final Instant expiresAt;

        AttemptWindow(int count, Instant expiresAt) {
            this.count = count;
            this.expiresAt = expiresAt;
        }
    }
}