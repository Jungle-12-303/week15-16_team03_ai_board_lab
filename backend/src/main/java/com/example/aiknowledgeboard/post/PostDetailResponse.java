package com.example.aiknowledgeboard.post;

import com.example.aiknowledgeboard.comment.CommentResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "게시글 상세 응답")
public record PostDetailResponse(
        @Schema(description = "게시글 ID", example = "1")
        Long id,
        @Schema(description = "게시글 제목", example = "Spring Boot 정리")
        String title,
        @Schema(description = "게시글 본문", example = "본문 전체 내용입니다.")
        String content,
        @Schema(description = "작성자 ID", example = "1")
        Long authorId,
        @Schema(description = "작성자 닉네임", example = "tester")
        String authorNickname,
        @Schema(description = "태그 목록", example = "[\"spring\", \"backend\"]")
        List<String> tags,
        @Schema(description = "댓글 목록")
        List<CommentResponse> comments,
        @Schema(description = "작성 시각", example = "2026-06-12T10:15:30.123Z")
        Instant createdAt,
        @Schema(description = "수정 시각", example = "2026-06-12T10:20:30.123Z")
        Instant updatedAt
) {
}
