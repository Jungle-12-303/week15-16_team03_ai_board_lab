package com.example.backend.rag;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OpenAiEmbeddingProperties {

    @Value("${openai.api-key:}")
    private String apiKey;

    @Value("${openai.embedding-model:text-embedding-3-small}")
    private String embeddingModel;

    @Value("${openai.embedding-dimensions:1536}")
    private int embeddingDimensions;

    @Value("${openai.chat-model:gpt-4.1-mini}")
    private String chatModel;

    public String getApiKey() {
        return apiKey;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public int getEmbeddingDimensions() {
        return embeddingDimensions;
    }

    public String getChatModel() {
        return chatModel;
    }
}
