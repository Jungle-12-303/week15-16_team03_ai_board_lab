package com.example.backend.post;

import com.example.backend.tag.Tag;

public class PostTagResponse {

    private Long id;
    private String name;

    public PostTagResponse(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public static PostTagResponse from(Tag tag) {
        return new PostTagResponse(tag.getId(), tag.getName());
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
