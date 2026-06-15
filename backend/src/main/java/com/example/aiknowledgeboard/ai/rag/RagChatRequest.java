package com.example.aiknowledgeboard.ai.rag;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "RAG 챗봇 질문 요청")
public record RagChatRequest(
        @Schema(description = "게시판 지식 베이스에 물어볼 질문", example = "우리 게시판에 주식 관련 게시글이 있어?")
        @NotBlank String message
) {
}
