package com.example.aiknowledgeboard.ai.notion;

public record NotionSaveResponse(
        String notionPageId,
        String notionUrl,
        boolean saved
) {
}
