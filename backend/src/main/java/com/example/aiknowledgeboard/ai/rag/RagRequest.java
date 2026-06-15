package com.example.aiknowledgeboard.ai.rag;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "RAG 유사 게시글 검색 요청")
public record RagRequest(
        @Schema(description = "유사 게시글을 찾을 기준 문장 또는 질문", example = "Spring Security JWT 인증 흐름")
        @NotBlank String query,
        @Schema(description = "검색 결과에서 제외할 게시글 ID. 글 작성 중 자기 자신을 제외할 때 사용합니다.", example = "1")
        Long excludePostId
) {
}
