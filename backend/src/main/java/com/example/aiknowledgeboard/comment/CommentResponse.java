package com.example.aiknowledgeboard.comment;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "댓글 응답")
public record CommentResponse(
        @Schema(description = "댓글 ID", example = "10")
        Long id,
        @Schema(description = "댓글이 달린 게시글 ID", example = "1")
        Long postId,
        @Schema(description = "댓글 작성자 ID", example = "2")
        Long authorId,
        @Schema(description = "댓글 작성자 닉네임", example = "commenter")
        String authorNickname,
        @Schema(description = "댓글 내용", example = "좋은 글입니다.")
        String content,
        @Schema(description = "작성 시각", example = "2026-06-12T10:15:30.123Z")
        Instant createdAt
) {
    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getPost().getId(),
                comment.getAuthor().getId(),
                comment.getAuthor().getNickname(),
                comment.getContent(),
                comment.getCreatedAt()
        );
    }
}
