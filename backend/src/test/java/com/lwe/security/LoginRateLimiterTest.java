package com.lwe.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRateLimiterTest {

    @Test
    void blocksAfterMaxFailures() {
        var limiter = new LoginRateLimiter(3, Duration.ofMinutes(1));

        limiter.recordFailure("1.2.3.4");
        limiter.recordFailure("1.2.3.4");
        assertThat(limiter.isBlocked("1.2.3.4")).isFalse();

        limiter.recordFailure("1.2.3.4");
        assertThat(limiter.isBlocked("1.2.3.4")).isTrue();
    }

    @Test
    void resetUnblocks() {
        var limiter = new LoginRateLimiter(1, Duration.ofMinutes(1));

        limiter.recordFailure("1.2.3.4");
        assertThat(limiter.isBlocked("1.2.3.4")).isTrue();

        limiter.reset("1.2.3.4");
        assertThat(limiter.isBlocked("1.2.3.4")).isFalse();
    }

    @Test
    void otherIpsUnaffected() {
        var limiter = new LoginRateLimiter(1, Duration.ofMinutes(1));

        limiter.recordFailure("1.2.3.4");

        assertThat(limiter.isBlocked("5.6.7.8")).isFalse();
    }

    @Test
    void cacheStaysBoundedUnderManyDistinctIps() throws Exception {
        var limiter = new LoginRateLimiter(100000, Duration.ofMinutes(10));

        for (int i = 0; i < 20_000; i++) {
            limiter.recordFailure("10.1." + (i / 256) + "." + (i % 256) + "-" + i);
        }

        var f = LoginRateLimiter.class.getDeclaredField("attempts");
        f.setAccessible(true);
        int size = ((java.util.Map<?, ?>) f.get(limiter)).size();
        assertThat(size).isLessThanOrEqualTo(10_000);
    }
}
