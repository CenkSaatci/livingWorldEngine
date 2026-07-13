package com.lwe;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Platzhalter-Test für das Skeleton. Vollständige {@code @SpringBootTest}-Tests werden in Phase 1
 * (P1-T03 Flyway + P1-T04 Auth) ergänzt — siehe {@code docs/TESTING.md}.
 */
class LweApplicationTests {

    @Test
    void applicationClassIsLoadable() {
        assertDoesNotThrow(() -> Class.forName("com.lwe.LweApplication"));
    }

    @Test
    void sanityCheck() {
        assertThat("LWE").isEqualTo("LWE");
    }
}