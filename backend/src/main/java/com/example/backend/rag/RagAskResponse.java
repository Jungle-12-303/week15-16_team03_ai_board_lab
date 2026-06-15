package com.example.backend.rag;

import lombok.Getter;

import java.util.List;

@Getter
public class RagAskResponse {

    private final String question;
    private final String answer;
    private final String answerModel;
    private final String embeddingModel;
    private final int totalMatches;
    private final List<RagReferenceResponse> references;

    public RagAskResponse(
        String question,
        String answer,
        String answerModel,
        String embeddingModel,
        int totalMatches,
        List<RagReferenceResponse> references
    ) {
        this.question = question;
        this.answer = answer;
        this.answerModel = answerModel;
        this.embeddingModel = embeddingModel;
        this.totalMatches = totalMatches;
        this.references = references;
    }
}
