package com.lwe.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "lwe.rate-limit")
public class RateLimitProperties {

    private int defaultMax = 100;
    private int defaultWindowSeconds = 60;
    private List<PathLimitConfig> paths = new ArrayList<>();

    public int getDefaultMax() { return defaultMax; }
    public void setDefaultMax(int v) { this.defaultMax = v; }
    public int getDefaultWindowSeconds() { return defaultWindowSeconds; }
    public void setDefaultWindowSeconds(int v) { this.defaultWindowSeconds = v; }
    public List<PathLimitConfig> getPaths() { return paths; }
    public void setPaths(List<PathLimitConfig> v) { this.paths = v; }

    public static class PathLimitConfig {
        private String pattern;
        private String method = "";
        private int max = 100;
        private int windowSeconds = 60;

        public String getPattern() { return pattern; }
        public void setPattern(String v) { this.pattern = v; }
        public String getMethod() { return method; }
        public void setMethod(String v) { this.method = v; }
        public int getMax() { return max; }
        public void setMax(int v) { this.max = v; }
        public int getWindowSeconds() { return windowSeconds; }
        public void setWindowSeconds(int v) { this.windowSeconds = v; }
    }
}
