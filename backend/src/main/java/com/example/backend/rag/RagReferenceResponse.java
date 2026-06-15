package com.example.backend.rag;

import com.example.backend.tag.Tag;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class RagReferenceResponse {

    private final Long postId;
    private final String title;
    private final String authorName;
    private final LocalDateTime createdAt;
    private final String contentPreview;
    private final String matchedChunkText;
    private final double similarityScore;
    private final List<Tag> tags;

    public RagReferenceResponse(
        Long postId,
        String title,
        String authorName,
        LocalDateTime createdAt,
        String contentPreview,
        String matchedChunkText,
        double similarityScore,
        List<Tag> tags
    ) {
        this.postId = postId;
        this.title = title;
        this.authorName = authorName;
        this.createdAt = createdAt;
        this.contentPreview = contentPreview;
        this.matchedChunkText = matchedChunkText;
        this.similarityScore = similarityScore;
        this.tags = tags;
    }
}
