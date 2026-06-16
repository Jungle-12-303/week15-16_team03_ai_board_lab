package com.jungle_choi.namanmu.service.rag;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jungle_choi.namanmu.config.OpenAiProperties;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class OpenAiEmbeddingClient {

    private final RestClient openAiRestClient;
    private final OpenAiProperties openAiProperties;

    public OpenAiEmbeddingClient(
            @Qualifier("openAiRestClient") RestClient openAiRestClient,
            OpenAiProperties openAiProperties) {
        this.openAiRestClient = openAiRestClient;
        this.openAiProperties = openAiProperties;
    }

    public EmbeddingResult createEmbedding(String input) {
        if (!openAiProperties.hasApiKey()) {
            throw new IllegalStateException("OPENAI_API_KEY is required to create embeddings.");
        }

        EmbeddingResponse response = openAiRestClient.post()
                .uri("/embeddings")
                .header(HttpHeaders.AUTHORIZATION, openAiProperties.authorizationHeader())
                .body(new EmbeddingRequest(openAiProperties.embeddingModel(), input))
                .retrieve()
                .body(EmbeddingResponse.class);

        return toResult(response);
    }

    public boolean isConfigured() {
        return openAiProperties.hasApiKey();
    }

    public String embeddingModel() {
        return openAiProperties.embeddingModel();
    }

    private static EmbeddingResult toResult(EmbeddingResponse response) {
        if (response == null || response.data() == null || response.data().isEmpty()) {
            throw new IllegalStateException("OpenAI embedding response has no data.");
        }

        EmbeddingData firstEmbedding = response.data().getFirst();

        if (firstEmbedding.embedding() == null || firstEmbedding.embedding().isEmpty()) {
            throw new IllegalStateException("OpenAI embedding response has no embedding.");
        }

        int promptTokens = 0;
        if (response.usage() != null) {
            promptTokens = response.usage().promptTokens();
        }

        return new EmbeddingResult(
                response.model(),
                firstEmbedding.embedding(),
                promptTokens);
    }

    private record EmbeddingRequest(String model, String input) {
    }

    private record EmbeddingResponse(
            List<EmbeddingData> data,
            String model,
            EmbeddingUsage usage) {
    }

    private record EmbeddingData(List<Double> embedding) {
    }

    private record EmbeddingUsage(@JsonProperty("prompt_tokens") int promptTokens) {
    }

    public record EmbeddingResult(
            String model,
            List<Double> embedding,
            int promptTokens) {

        public int dimensions() {
            return embedding.size();
        }
    }
}
