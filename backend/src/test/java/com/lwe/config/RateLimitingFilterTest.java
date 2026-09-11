package com.lwe.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RateLimitingFilterTest {

    private RateLimitProperties props(int max, int windowSeconds) {
        var p = new RateLimitProperties();
        p.setDefaultMax(max);
        p.setDefaultWindowSeconds(windowSeconds);
        return p;
    }

    private MockHttpServletRequest req(String ip, String path) {
        var r = new MockHttpServletRequest("GET", path);
        r.setRemoteAddr(ip);
        return r;
    }

    @Test
    void rejectsOverLimitWith429AndRetryAfter() throws Exception {
        var filter = new RateLimitingFilter(props(2, 60));
        var chain = mock(FilterChain.class);

        filter.doFilter(req("1.2.3.4", "/api/v1/worlds"), new MockHttpServletResponse(), chain);
        filter.doFilter(req("1.2.3.4", "/api/v1/worlds"), new MockHttpServletResponse(), chain);

        var res = new MockHttpServletResponse();
        filter.doFilter(req("1.2.3.4", "/api/v1/worlds"), res, chain);

        assertThat(res.getStatus()).isEqualTo(429);
        assertThat(res.getHeader("Retry-After")).isNotNull();
        assertThat(res.getContentAsString()).contains("RATE_LIMIT_EXCEEDED");
    }

    @Test
    void cacheStaysBoundedUnderManyDistinctKeys() throws Exception {
        var filter = new RateLimitingFilter(props(100000, 60));
        var chain = mock(FilterChain.class);

        for (int i = 0; i < 20_000; i++) {
            filter.doFilter(req("10.0.0." + (i % 256) + "." + i, "/api/v1/x" + i),
                new MockHttpServletResponse(), chain);
        }

        assertThat(cacheSize(filter)).isLessThanOrEqualTo(10_000);
    }

    @SuppressWarnings("unchecked")
    private static int cacheSize(RateLimitingFilter filter) throws Exception {
        var f = RateLimitingFilter.class.getDeclaredField("attempts");
        f.setAccessible(true);
        return ((java.util.Map<?, ?>) f.get(filter)).size();
    }
}
