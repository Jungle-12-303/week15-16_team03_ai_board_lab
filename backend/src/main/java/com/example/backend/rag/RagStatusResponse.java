package com.example.backend.rag;

public class RagStatusResponse {

    private final boolean apiKeyConfigured;
    private final String embeddingModel;
    private final String chatModel;
    private final int embeddingDimensions;
    private final long indexedPostCount;
    private final long embeddingRowCount;

    public RagStatusResponse(
        boolean apiKeyConfigured,
        String embeddingModel,
        String chatModel,
        int embeddingDimensions,
        long indexedPostCount,
        long embeddingRowCount
    ) {
        this.apiKeyConfigured = apiKeyConfigured;
        this.embeddingModel = embeddingModel;
        this.chatModel = chatModel;
        this.embeddingDimensions = embeddingDimensions;
        this.indexedPostCount = indexedPostCount;
        this.embeddingRowCount = embeddingRowCount;
    }

    public boolean isApiKeyConfigured() {
        return apiKeyConfigured;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public String getChatModel() {
        return chatModel;
    }

    public int getEmbeddingDimensions() {
        return embeddingDimensions;
    }

    public long getIndexedPostCount() {
        return indexedPostCount;
    }

    public long getEmbeddingRowCount() {
        return embeddingRowCount;
    }
}
