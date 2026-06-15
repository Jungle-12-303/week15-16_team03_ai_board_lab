package com.example.backend.rag;

import lombok.Getter;

@Getter
public class RagSearchMatch {

    private final Long postId;
    private final String chunkText;
    private final double distance;

    public RagSearchMatch(Long postId, String chunkText, double distance) {
        this.postId = postId;
        this.chunkText = chunkText;
        this.distance = distance;
    }
}
