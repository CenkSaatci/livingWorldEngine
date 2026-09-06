package com.lwe.i18n;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class I18nConfigTest {

    private final I18nConfig config = new I18nConfig();

    @Test
    void shouldResolveGermanFromHeader() {
        var resolver = config.localeResolver();
        var request = new MockHttpServletRequest();
        request.addHeader("Accept-Language", "de");

        var locale = resolver.resolveLocale(request);
        assertThat(locale.getLanguage()).isEqualTo("de");
    }

    @Test
    void shouldResolveEnglishFromHeader() {
        var resolver = config.localeResolver();
        var request = new MockHttpServletRequest();
        request.addHeader("Accept-Language", "en-US");

        var locale = resolver.resolveLocale(request);
        assertThat(locale.getLanguage()).isEqualTo("en");
    }

    @Test
    void shouldFallbackToGermanForUnknownLocale() {
        var resolver = config.localeResolver();
        var request = new MockHttpServletRequest();
        request.addHeader("Accept-Language", "fr");

        var locale = resolver.resolveLocale(request);
        // Fallback = de (Default)
        assertThat(locale.getLanguage()).isEqualTo("de");
    }

    @Test
    void shouldUseDefaultWhenNoHeader() {
        var resolver = config.localeResolver();
        var request = new MockHttpServletRequest();

        var locale = resolver.resolveLocale(request);
        assertThat(locale.getLanguage()).isEqualTo("de");
    }
}