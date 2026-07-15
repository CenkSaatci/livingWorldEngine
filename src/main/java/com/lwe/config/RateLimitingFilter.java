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

@Component
@Order(1)
public class RateLimitingFilter implements Filter {

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

        if (window.count > limit.max) {
            var httpRes = (HttpServletResponse) response;
            httpRes.setStatus(429);
            httpRes.setContentType("application/json");
            httpRes.getWriter().write(
                "{\"error\":{\"code\":\"RATE_LIMIT_EXCEEDED\",\"message\":\"Too many requests\"}}");
            return;
        }

        chain.doFilter(request, response);
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
