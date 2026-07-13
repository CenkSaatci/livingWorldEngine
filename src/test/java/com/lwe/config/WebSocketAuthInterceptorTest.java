package com.lwe.config;

import com.lwe.security.JwtService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prüft, dass {@link WebSocketAuthInterceptor} instanziiert werden kann
 * und seine Grundstruktur korrekt ist. Die funktionale WS-Auth wird
 * via E2E-Integrationstest in P1-T08 verifiziert.
 */
class WebSocketAuthInterceptorTest {

    @Test
    void shouldBeInstantiatable() {
        var jwtService = new JwtService(
            "test-secret-key-which-must-be-at-least-32-characters-long-here", 24, 7);
        var interceptor = new WebSocketAuthInterceptor(jwtService);
        assertThat(interceptor).isNotNull();
    }
}