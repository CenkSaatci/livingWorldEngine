package com.lwe.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(1)
public class RateLimitingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    /** Harte Obergrenze gegen unbegrenztes Wachstum (ein Eintrag pro ip:path). */
    static final int MAX_ENTRIES = 10_000;

    private final RateLimitProperties properties;
    private final Map<String, MutableWindow> attempts = new ConcurrentHashMap<>();

    public RateLimitingFilter(RateLimitProperties properties) {
        this.properties = properties;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        var httpReq = (HttpServletRequest) request;
        var ip = httpReq.getRemoteAddr();
        var path = httpReq.getRequestURI();
        var method = httpReq.getMethod();
        var now = Instant.now();

        var limit = resolveLimit(path, method);
        var key = ip + ":" + path;

        var window = attempts.compute(key, (k, w) -> {
            if (w == null || w.expiresAt.isBefore(now)) {
                return new MutableWindow(1, now.plus(limit.window));
            }
            w.count++;
            return w;
        });
        if (attempts.size() > MAX_ENTRIES) evictExpired(now);

        log.debug("RATE: {} {} key={} count={} max={}", method, path, key, window.count, limit.max);

        if (window.count > limit.max) {
            var httpRes = (HttpServletResponse) response;
            var retryAfter = Math.max(1, window.expiresAt.getEpochSecond() - now.getEpochSecond());
            httpRes.setStatus(429);
            httpRes.setHeader("Retry-After", String.valueOf(retryAfter));
            httpRes.setContentType("application/json");
            httpRes.getWriter().write(
                "{\"error\":{\"code\":\"RATE_LIMIT_EXCEEDED\",\"message\":\"Too many requests\"}}");
            return;
        }

        chain.doFilter(request, response);
    }

    /** Entfernt abgelaufene Fenster; als Notbremse auch die ältesten Einträge. */
    private void evictExpired(Instant now) {
        attempts.entrySet().removeIf(e -> e.getValue().expiresAt.isBefore(now));
        if (attempts.size() > MAX_ENTRIES) {
            var it = attempts.keySet().iterator();
            while (attempts.size() > MAX_ENTRIES && it.hasNext()) {
                it.next();
                it.remove();
            }
        }
    }

    private Limit resolveLimit(String path, String method) {
        for (var pl : properties.getPaths()) {
            if (!pl.getMethod().isEmpty() && !pl.getMethod().equalsIgnoreCase(method)) continue;
            if (matchPath(pl.getPattern(), path)) return new Limit(pl.getMax(), Duration.ofSeconds(pl.getWindowSeconds()));
        }
        return new Limit(properties.getDefaultMax(), Duration.ofSeconds(properties.getDefaultWindowSeconds()));
    }

    private boolean matchPath(String pattern, String path) {
        var regex = pattern
            .replace(".", "\\.")
            .replace("**", ".+")
            .replace("*", "[^/]+");
        return path.matches(regex);
    }

    record Limit(int max, Duration window) {}

    private static class MutableWindow {
        int count;
        final Instant expiresAt;
        MutableWindow(int count, Instant expiresAt) {
            this.count = count;
            this.expiresAt = expiresAt;
        }
    }
}
