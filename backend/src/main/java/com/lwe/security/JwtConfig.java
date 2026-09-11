package com.lwe.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;

/**
 * Konfiguriert den JwtService als Spring-Bean mit Werten aus application.yml.
 */
@Configuration
public class JwtConfig {

    /** Dev-Default aus application.yml — darf in prod niemals verwendet werden. */
    static final String DEV_DEFAULT_SECRET = "dev-only-change-me-please-32-chars-minimum-xxx";

    private final Environment env;

    public JwtConfig(Environment env) {
        this.env = env;
    }

    @Bean
    public JwtService jwtService(
        @org.springframework.beans.factory.annotation.Value("${lwe.jwt.secret}") String secret,
        @org.springframework.beans.factory.annotation.Value("${lwe.jwt.access-token-expiration-hours}") long accessExp,
        @org.springframework.beans.factory.annotation.Value("${lwe.jwt.refresh-token-expiration-days}") long refreshExp
    ) {
        if (isProd() && (secret == null || secret.isBlank() || DEV_DEFAULT_SECRET.equals(secret))) {
            throw new IllegalStateException(
                "JWT_SECRET muss in prod gesetzt sein (kein Dev-Default erlaubt)");
        }
        return new JwtService(secret, accessExp, refreshExp);
    }

    private boolean isProd() {
        return Arrays.asList(env.getActiveProfiles()).contains("prod");
    }
}