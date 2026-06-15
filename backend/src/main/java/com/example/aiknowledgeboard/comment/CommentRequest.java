package com.example.aiknowledgeboard.comment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "댓글 작성 요청")
public record CommentRequest(
        @Schema(description = "댓글 내용", example = "좋은 글입니다.", maxLength = 1000)
        @NotBlank @Size(max = 1000) String content
) {
}
