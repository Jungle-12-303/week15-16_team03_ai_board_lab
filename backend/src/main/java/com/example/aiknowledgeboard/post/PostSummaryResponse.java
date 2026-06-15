package com.example.aiknowledgeboard.post;

import com.example.aiknowledgeboard.tag.TagService;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "게시글 목록용 요약 응답")
public record PostSummaryResponse(
        @Schema(description = "게시글 ID", example = "1")
        Long id,
        @Schema(description = "게시글 제목", example = "Spring Boot 정리")
        String title,
        @Schema(description = "목록 화면용 본문 미리보기", example = "Spring Boot 게시판 구현 내용...")
        String contentPreview,
        @Schema(description = "작성자 ID", example = "1")
        Long authorId,
        @Schema(description = "작성자 닉네임", example = "tester")
        String authorNickname,
        @Schema(description = "태그 목록", example = "[\"spring\", \"backend\"]")
        List<String> tags,
        @Schema(description = "댓글 수", example = "2")
        long commentCount,
        @Schema(description = "작성 시각", example = "2026-06-12T10:15:30.123Z")
        Instant createdAt,
        @Schema(description = "수정 시각", example = "2026-06-12T10:20:30.123Z")
        Instant updatedAt
) {

    /*
    PostSummaryResponse는 단순히 Post 값을 그대로 복사하는 게 아니야.
    중간에 변환이 들어가.
    content 전체 → contentPreview로 140자만 자름
    author 객체 → authorId, authorNickname으로 나눔
    tags 객체 목록 → 태그 이름 목록으로 바꿈
    댓글 목록 → commentCount 숫자로 바꿈
    */
    public static PostSummaryResponse from(Post post, TagService tagService, long commentCount) {
        String content = post.getContent();
        String preview = content.length() <= 140 ? content : content.substring(0, 140) + "...";
        return new PostSummaryResponse(
                post.getId(),
                post.getTitle(),
                preview,
                post.getAuthor().getId(),
                post.getAuthor().getNickname(),
                tagService.toNames(post.getTags()),
                commentCount,
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
