package com.jungle_choi.namanmu.api.dto;

import java.util.List;

public record PostResponse(
        Long id,
        String author,
        String category,
        String createdAt,
        String title,
        String content,
        List<String> tags,
        List<CommentResponse> comments) {
}
