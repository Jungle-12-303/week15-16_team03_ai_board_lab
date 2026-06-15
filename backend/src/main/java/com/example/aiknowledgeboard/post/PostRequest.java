package com.example.aiknowledgeboard.post;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "게시글 작성/수정 요청")
public record PostRequest(
        @Schema(description = "게시글 제목", example = "Spring Boot 정리", maxLength = 200)
        @NotBlank @Size(max = 200) String title,
        @Schema(description = "게시글 본문", example = "Spring Boot로 게시판 API를 구현한 내용입니다.")
        @NotBlank String content,
        @Schema(description = "게시글 태그 목록", example = "[\"spring\", \"backend\"]")
        List<String> tags
) {
}
