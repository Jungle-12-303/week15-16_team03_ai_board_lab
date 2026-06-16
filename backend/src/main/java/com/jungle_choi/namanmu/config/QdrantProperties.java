package com.jungle_choi.namanmu.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.qdrant")
public record QdrantProperties(
        boolean enabled,
        String baseUrl,
        String postCollection,
        String chunkCollection,
        int requestTimeoutSeconds,
        int searchLimit) {

    public Duration requestTimeout() {
        return Duration.ofSeconds(Math.max(requestTimeoutSeconds, 1));
    }

    public int normalizedSearchLimit() {
        return Math.max(searchLimit, 1);
    }
}
