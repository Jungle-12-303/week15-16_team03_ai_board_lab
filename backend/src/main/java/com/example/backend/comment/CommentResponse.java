package com.example.backend.comment;

import java.time.LocalDateTime;

public class CommentResponse {

    private Long id;
    private String content;
    private String authorName;
    private LocalDateTime createdAt;
    private String ownerLoginId;

    public CommentResponse(
        Long id,
        String content,
        String authorName,
        LocalDateTime createdAt,
        String ownerLoginId
    ) {
        this.id = id;
        this.content = content;
        this.authorName = authorName;
        this.createdAt = createdAt;
        this.ownerLoginId = ownerLoginId;
    }

    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
            comment.getId(),
            comment.getContent(),
            comment.getAuthorName(),
            comment.getCreatedAt(),
            comment.getOwnerLoginId()
        );
    }

    public Long getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public String getAuthorName() {
        return authorName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getOwnerLoginId() {
        return ownerLoginId;
    }
}
