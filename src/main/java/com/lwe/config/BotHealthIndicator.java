package com.lwe.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Health-Indicator für den AI-Bot. Sendet einen GET-Request an
 * {@code AI_BOT_URL + /health}. Der Bot ist UP, wenn er mit 200 antwortet.
 */
@Component
public class BotHealthIndicator implements HealthIndicator {

    private final String botUrl;
    private final HttpClient client;

    public BotHealthIndicator() {
        this.botUrl = System.getenv("AI_BOT_URL");
        this.client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    }

    @Override
    public Health health() {
        if (botUrl == null || botUrl.isBlank()) {
            return Health.unknown().withDetail("reason", "AI_BOT_URL not set").build();
        }

        try {
            var req = HttpRequest.newBuilder()
                .uri(URI.create(botUrl + "/health"))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

            var res = client.send(req, HttpResponse.BodyHandlers.ofString());

            if (res.statusCode() == 200) {
                return Health.up().withDetail("url", botUrl).build();
            }
            return Health.down()
                .withDetail("url", botUrl)
                .withDetail("status", res.statusCode())
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("url", botUrl)
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
