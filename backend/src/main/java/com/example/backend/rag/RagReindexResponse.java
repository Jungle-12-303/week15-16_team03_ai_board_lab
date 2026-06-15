package com.example.backend.rag;

import lombok.Getter;

import java.util.List;

@Getter
public class RagReindexResponse {

    private final String requestedBy;
    private final String embeddingModel;
    private final int totalPosts;
    private final int successCount;
    private final int failedCount;
    private final List<Long> failedPostIds;

    public RagReindexResponse(
        String requestedBy,
        String embeddingModel,
        int totalPosts,
        int successCount,
        int failedCount,
        List<Long> failedPostIds
    ) {
        this.requestedBy = requestedBy;
        this.embeddingModel = embeddingModel;
        this.totalPosts = totalPosts;
        this.successCount = successCount;
        this.failedCount = failedCount;
        this.failedPostIds = failedPostIds;
    }
}
