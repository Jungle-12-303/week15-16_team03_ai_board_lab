package com.example.backend.post;

import java.time.LocalDateTime;
import java.util.List;

public class PostResponse {

    private Long id;
    private String title;
    private String content;
    private String authorName;
    private LocalDateTime createdAt;
    private String ownerLoginId;
    private List<PostTagResponse> tags;

    public PostResponse(
        Long id,
        String title,
        String content,
        String authorName,
        LocalDateTime createdAt,
        String ownerLoginId,
        List<PostTagResponse> tags
    ) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.authorName = authorName;
        this.createdAt = createdAt;
        this.ownerLoginId = ownerLoginId;
        this.tags = tags;
    }

    public static PostResponse from(Post post) {
        return new PostResponse(
            post.getId(),
            post.getTitle(),
            post.getContent(),
            post.getAuthorName(),
            post.getCreatedAt(),
            post.getOwnerLoginId(),
            post.getTags().stream().map(PostTagResponse::from).toList()
        );
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
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

    public List<PostTagResponse> getTags() {
        return tags;
    }
}
