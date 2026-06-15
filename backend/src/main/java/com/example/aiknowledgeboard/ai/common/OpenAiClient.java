package com.example.aiknowledgeboard.ai.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class OpenAiClient {
    private final String apiKey;
    private final String chatModel;
    private final String embeddingModel;
    private final int dimensions;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public OpenAiClient(
            @Value("${openai.api-key}") String apiKey,
            @Value("${openai.chat-model}") String chatModel,
            @Value("${openai.embedding-model}") String embeddingModel,
            @Value("${app.ai.embedding-dimensions}") int dimensions,
            ObjectMapper objectMapper
    ) {
        this.apiKey = apiKey;
        this.chatModel = chatModel;
        this.embeddingModel = embeddingModel;
        this.dimensions = dimensions;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().baseUrl("https://api.openai.com/v1").build();
    }

    public double[] createEmbedding(String text) {
        if (apiKey == null || apiKey.isBlank()) {
            return createLocalEmbedding(text);
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", embeddingModel);
            body.put("input", text);
            Map<String, Object> response = restClient.post()
                    .uri("/embeddings")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            List<?> data = (List<?>) response.get("data");
            Map<?, ?> first = (Map<?, ?>) data.getFirst();
            List<?> embedding = (List<?>) first.get("embedding");
            double[] vector = new double[dimensions];
            for (int i = 0; i < Math.min(dimensions, embedding.size()); i++) {
                vector[i] = ((Number) embedding.get(i)).doubleValue();
            }
            return vector;
        } catch (Exception ex) {
            return createLocalEmbedding(text);
        }
    }

    public String chat(String systemPrompt, String userPrompt, String fallback) {
        if (apiKey == null || apiKey.isBlank()) {
            return fallback;
        }
        try {
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            messages.add(Map.of("role", "user", "content", userPrompt));

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", chatModel);
            body.put("messages", messages);
            body.put("temperature", 0.2);

            Map<String, Object> response = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            List<?> choices = (List<?>) response.get("choices");
            Map<?, ?> first = (Map<?, ?>) choices.getFirst();
            Map<?, ?> message = (Map<?, ?>) first.get("message");
            return String.valueOf(message.get("content"));
        } catch (Exception ex) {
            return fallback;
        }
    }

    public String toVectorLiteral(double[] vector) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(vector[i]);
        }
        return builder.append(']').toString();
    }

    private double[] createLocalEmbedding(String text) {
        double[] vector = new double[dimensions];
        String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT);
        for (String token : normalized.split("[^a-z0-9가-힣]+")) {
            if (token.isBlank()) {
                continue;
            }
            String hash = sha256(token);
            int index = Math.floorMod(hash.hashCode(), dimensions);
            int sign = hash.charAt(0) % 2 == 0 ? 1 : -1;
            vector[index] += sign;
        }
        normalize(vector);
        return vector;
    }

    private String sha256(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            return Integer.toString(text.hashCode());
        }
    }

    private void normalize(double[] vector) {
        double sum = 0.0;
        for (double value : vector) {
            sum += value * value;
        }
        double norm = Math.sqrt(sum);
        if (norm == 0.0) {
            return;
        }
        for (int i = 0; i < vector.length; i++) {
            vector[i] = vector[i] / norm;
        }
    }
}
