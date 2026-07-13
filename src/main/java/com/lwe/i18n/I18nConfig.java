package com.lwe.i18n;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.List;
import java.util.Locale;

/**
 * Konfiguriert die Locale-Auflösung über den {@code Accept-Language}-Header.
 *
 * <p>Fallback-Locale: {@code de}. Nicht unterstützte Locales fallen auf {@code en} zurück.
 * Siehe {@code docs/ADR/007-internationalization-strategy.md}.
 */
@Configuration
public class I18nConfig {

    private static final List<Locale> SUPPORTED_LOCALES = List.of(
        Locale.GERMAN, Locale.ENGLISH
    );

    @Bean
    public AcceptHeaderLocaleResolver localeResolver() {
        var resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.GERMAN);
        resolver.setSupportedLocales(SUPPORTED_LOCALES);
        return resolver;
    }
}