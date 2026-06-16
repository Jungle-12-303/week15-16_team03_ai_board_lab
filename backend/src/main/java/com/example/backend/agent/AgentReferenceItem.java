package com.example.backend.agent;

public class AgentReferenceItem {

    private Long postId;
    private String title;

    public AgentReferenceItem() {
    }

    public AgentReferenceItem(Long postId, String title) {
        this.postId = postId;
        this.title = title;
    }

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
