package com.example.backend.rag;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class OpenAiEmbeddingClient {

    private final RestClient restClient;
    private final OpenAiEmbeddingProperties properties;

    public OpenAiEmbeddingClient(OpenAiEmbeddingProperties properties) {
        this.restClient = RestClient.builder()
            .baseUrl("https://api.openai.com/v1")
            .build();
        this.properties = properties;
    }

    public boolean isConfigured() {
        return properties.getApiKey() != null && !properties.getApiKey().isBlank();
    }

    public String getEmbeddingModel() {
        return properties.getEmbeddingModel();
    }

    public int getEmbeddingDimensions() {
        return properties.getEmbeddingDimensions();
    }

    public List<Double> createEmbedding(String text, String user) {
        if (!isConfigured()) {
            throw new IllegalStateException("OPENAI_API_KEY가 설정되지 않았습니다.");
        }

        OpenAiEmbeddingRequest request = new OpenAiEmbeddingRequest();
        request.setInput(text);
        request.setModel(properties.getEmbeddingModel());
        request.setDimensions(properties.getEmbeddingDimensions());
        request.setEncodingFormat("float");
        request.setUser(user);

        OpenAiEmbeddingResponse response = restClient.post()
            .uri("/embeddings")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .body(OpenAiEmbeddingResponse.class);

        if (response == null || response.getData() == null || response.getData().isEmpty()) {
            throw new IllegalStateException("임베딩 응답이 비어 있습니다.");
        }

        return response.getData().get(0).getEmbedding();
    }
}
