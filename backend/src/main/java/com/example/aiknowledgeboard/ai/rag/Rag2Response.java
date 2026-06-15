package com.example.aiknowledgeboard.ai.rag;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "RAG 유사 게시글 검색 요청")
public record Rag2Response(
    String requestQuery

) {
}