package com.lwe.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor authInterceptor;
    private final List<String> allowedOrigins;

    public WebSocketConfig(WebSocketAuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
        // Gleiche Quelle wie die HTTP-CORS-Regeln (SecurityConfig): ohne explizite
        // CORS_ALLOWED_ORIGINS im Dev-Betrieb offen (`*`), sonst die konfigurierte Liste.
        // Vorher stand hier `lwe.cors.allowed-origins` mit localhost-Default — dadurch
        // schlug der WS-Handshake bei Zugriff über die LAN-IP mit 403 fehl.
        var env = System.getenv("CORS_ALLOWED_ORIGINS");
        this.allowedOrigins = (env == null || env.isBlank())
            ? List.of("*")
            : List.of(env.split(","));
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // ADR-015: /queue für transiente Direktnachrichten (POI-Aktion "actor"-Chat).
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns(allowedOrigins.toArray(String[]::new));
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authInterceptor);
    }
}
