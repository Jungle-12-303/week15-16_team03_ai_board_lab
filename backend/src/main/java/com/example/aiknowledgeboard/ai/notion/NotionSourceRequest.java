package com.example.aiknowledgeboard.ai.notion;

public record NotionSourceRequest(
        Long id,
        String title,
        String contentPreview,
        String authorNickname,
        Double score,
        String link
) {
}
