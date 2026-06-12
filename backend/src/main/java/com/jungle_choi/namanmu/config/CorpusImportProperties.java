package com.jungle_choi.namanmu.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.corpus-import")
public record CorpusImportProperties(
        boolean enabled,
        String sourceFile,
        String authorName,
        int maxItems,
        int minContentLength,
        int maxContentLength,
        int requestTimeoutSeconds,
        int requestDelayMillis,
        boolean exitAfterRun,
        String userAgent) {

    public String normalizedSourceFile() {
        if (sourceFile == null || sourceFile.isBlank()) {
            return "src/main/resources/corpus-sources.tsv";
        }

        return sourceFile.trim();
    }

    public String normalizedAuthorName() {
        if (authorName == null || authorName.isBlank()) {
            return "crawler";
        }

        return authorName.trim();
    }

    public int normalizedMaxItems() {
        if (maxItems <= 0) {
            return 20;
        }

        return maxItems;
    }

    public int normalizedMinContentLength() {
        if (minContentLength <= 0) {
            return 500;
        }

        return minContentLength;
    }

    public int normalizedMaxContentLength() {
        if (maxContentLength <= 0) {
            return 12000;
        }

        return Math.max(maxContentLength, normalizedMinContentLength());
    }

    public Duration requestTimeout() {
        return Duration.ofSeconds(Math.max(requestTimeoutSeconds, 1));
    }

    public Duration requestDelay() {
        return Duration.ofMillis(Math.max(requestDelayMillis, 0));
    }

    public String normalizedUserAgent() {
        if (userAgent == null || userAgent.isBlank()) {
            return "ProjectAlphaCorpusImporter/1.0";
        }

        return userAgent.trim();
    }
}
