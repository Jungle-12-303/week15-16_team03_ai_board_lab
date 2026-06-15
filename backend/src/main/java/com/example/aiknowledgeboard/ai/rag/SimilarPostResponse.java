package com.example.aiknowledgeboard.ai.rag;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "유사 게시글 출처 응답")
public record SimilarPostResponse(
        @Schema(description = "게시글 ID", example = "1")
        Long id,
        @Schema(description = "게시글 제목", example = "Spring Security JWT 정리")
        String title,
        @Schema(description = "본문 미리보기", example = "JWT 필터는 요청마다 Authorization 헤더를 확인합니다...")
        String contentPreview,
        @Schema(description = "작성자 닉네임", example = "tester")
        String authorNickname,
        @Schema(description = "유사도 점수. 값이 클수록 더 유사하게 계산된 결과입니다.", example = "0.87")
        double score,
        @Schema(description = "프론트엔드에서 이동할 수 있는 게시글 링크", example = "/posts/1")
        String link
) {
}
