package com.lwe.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Globaler IP-basierter Rate-Limiter (max 100 Requests/Minute pro IP).
 */
@Component
@Order(1)
public class RateLimitingFilter implements Filter {

    private static final int MAX_REQUESTS = 100;
    private static final Duration WINDOW = Duration.ofMinutes(1);
    private final Map<String, MutableWindow> attempts = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        var httpReq = (HttpServletRequest) request;
        var ip = httpReq.getRemoteAddr();
        var now = Instant.now();

        var window = attempts.compute(ip, (key, w) -> {
            if (w == null || w.expiresAt.isBefore(now)) {
                return new MutableWindow(1, now.plus(WINDOW));
            }
            w.count++;
            return w;
        });

        if (window.count > MAX_REQUESTS) {
            var httpRes = (HttpServletResponse) response;
            httpRes.setStatus(429);
            httpRes.setContentType("application/json");
            httpRes.getWriter().write(
                "{\"error\":{\"code\":\"RATE_LIMIT_EXCEEDED\",\"message\":\"Too many requests\"}}");
            return;
        }

        chain.doFilter(request, response);
    }

    private static class MutableWindow {
        int count;
        final Instant expiresAt;

        MutableWindow(int count, Instant expiresAt) {
            this.count = count;
            this.expiresAt = expiresAt;
        }
    }
}
