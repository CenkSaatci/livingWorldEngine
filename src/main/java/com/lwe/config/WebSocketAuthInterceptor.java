package com.lwe.config;

import com.lwe.security.JwtService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.security.Principal;

/**
 * Interceptiert STOMP-CONNECT-Frames und validiert JWT aus {@code Authorization}-Header.
 *
 * <p>Bei Erfolg: {@link Principal} wird auf die User-ID gesetzt.
 * Bei Fehler: Session-Attribut {@code ws_auth_failed} wird gesetzt → Verbindung getrennt.
 */
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;

    public WebSocketAuthInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        var accessor = StompHeaderAccessor.wrap(message);
        if (accessor.getCommand() != StompCommand.CONNECT) {
            return message;
        }

        var authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            accessor.setSessionAttributes(java.util.Map.of("ws_auth_failed", true));
            return message;
        }

        var token = authHeader.substring(7);
        try {
            var userId = jwtService.extractUserId(token);
            accessor.setUser(() -> userId);
            return message;
        } catch (JwtService.TokenExpiredException | JwtService.TokenInvalidException e) {
            accessor.setSessionAttributes(java.util.Map.of("ws_auth_failed", true));
            return message;
        }
    }
}