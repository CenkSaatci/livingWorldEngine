package com.lwe.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Konfiguriert den JwtService als Spring-Bean mit Werten aus application.yml.
 */
@Configuration
public class JwtConfig {

    @Bean
    public JwtService jwtService(
        @org.springframework.beans.factory.annotation.Value("${lwe.jwt.secret}") String secret,
        @org.springframework.beans.factory.annotation.Value("${lwe.jwt.access-token-expiration-hours}") long accessExp,
        @org.springframework.beans.factory.annotation.Value("${lwe.jwt.refresh-token-expiration-days}") long refreshExp
    ) {
        return new JwtService(secret, accessExp, refreshExp);
    }
}