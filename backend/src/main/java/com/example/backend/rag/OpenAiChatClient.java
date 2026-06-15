package com.example.backend.rag;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class OpenAiChatClient {

    private final RestClient restClient;
    private final OpenAiEmbeddingProperties properties;

    public OpenAiChatClient(OpenAiEmbeddingProperties properties) {
        this.restClient = RestClient.builder()
            .baseUrl("https://api.openai.com/v1")
            .build();
        this.properties = properties;
    }

    public boolean isConfigured() {
        return properties.getApiKey() != null && !properties.getApiKey().isBlank();
    }

    public String getChatModel() {
        return properties.getChatModel();
    }

    public String createAnswer(String developerMessage, String userMessage, String user) {
        if (!isConfigured()) {
            throw new IllegalStateException("OPENAI_API_KEY가 설정되지 않았습니다.");
        }

        OpenAiChatRequest request = new OpenAiChatRequest();
        request.setModel(properties.getChatModel());
        request.setMessages(List.of(
            new OpenAiChatMessage("developer", developerMessage),
            new OpenAiChatMessage("user", userMessage)
        ));
        request.setMaxCompletionTokens(700);
        request.setTemperature(0.2);
        request.setUser(user);

        OpenAiChatResponse response = restClient.post()
            .uri("/chat/completions")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .body(OpenAiChatResponse.class);

        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            throw new IllegalStateException("답변 생성 응답이 비어 있습니다.");
        }

        OpenAiChatMessage message = response.getChoices().get(0).getMessage();

        if (message == null || message.getContent() == null || message.getContent().isBlank()) {
            throw new IllegalStateException("답변 생성 결과가 비어 있습니다.");
        }

        return message.getContent().trim();
    }
}
