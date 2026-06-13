package com.jungle_choi.namanmu.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.embedding-worker")
public record EmbeddingWorkerProperties(
        boolean enabled,
        long fixedDelayMillis,
        int batchSize) {

    public int normalizedBatchSize() {
        return Math.min(Math.max(batchSize, 1), 20);
    }
}
