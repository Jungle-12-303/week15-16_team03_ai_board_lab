package com.example.aiknowledgeboard.ai.rag;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "RAG 유사 게시글 검색 응답")
public record RagResponse(
        @Schema(description = "유사 글을 바탕으로 만든 요약 또는 안내 메시지", example = "JWT 인증 흐름과 관련된 글 3개를 찾았습니다.")
        String summary,
        @Schema(description = "유사 게시글 출처 목록")
        List<SimilarPostResponse> sources
) {
}
