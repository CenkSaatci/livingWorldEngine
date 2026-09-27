package com.lwe.config;

import com.lwe.security.JwtAuthFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Regression (Audit H-1): HTTP-CORS und WebSocket müssen dieselbe Quelle nutzen
 * ({@code lwe.cors.allowed-origins}). Vorher las SecurityConfig die Env direkt und
 * ignorierte die Property — im Prod-Profil blieb dadurch {@code *} aktiv.
 */
class CorsOriginConfigTest {

    @Test
    void securityConfigUsesConfiguredOrigins() {
        var config = new SecurityConfig(mock(JwtAuthFilter.class),
            "https://example.com,https://app.example.com");
        var source = (UrlBasedCorsConfigurationSource) config.corsConfigurationSource();

        var cors = source.getCorsConfiguration(new MockHttpServletRequest("GET", "/api/v1/game-systems"));

        assertThat(cors).isNotNull();
        assertThat(cors.getAllowedOriginPatterns())
            .containsExactly("https://example.com", "https://app.example.com");
    }

    @Test
    void webSocketUsesConfiguredOrigins() {
        var registry = mock(StompEndpointRegistry.class);
        var registration = mock(StompWebSocketEndpointRegistration.class);
        when(registry.addEndpoint("/ws")).thenReturn(registration);

        new WebSocketConfig(mock(WebSocketAuthInterceptor.class), "https://example.com")
            .registerStompEndpoints(registry);

        verify(registration).setAllowedOriginPatterns("https://example.com");
    }
}
