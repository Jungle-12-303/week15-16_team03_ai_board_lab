package com.jungle_choi.namanmu.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.openai")
public record OpenAiProperties(
        String apiKey,
        String baseUrl,
        String chatModel,
        String embeddingModel,
        int requestTimeoutSeconds) {

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String authorizationHeader() {
        return "Bearer " + apiKey;
    }

    public Duration requestTimeout() {
        return Duration.ofSeconds(Math.max(requestTimeoutSeconds, 1));
    }
}
