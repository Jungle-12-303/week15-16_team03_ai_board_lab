package com.example.aiknowledgeboard.common;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(description = "페이지 응답")
public record PageResponse<T>(
        @Schema(description = "현재 페이지의 데이터 목록")
        List<T> content,
        @Schema(description = "0부터 시작하는 현재 페이지 번호", example = "0")
        int page,
        @Schema(description = "페이지 크기", example = "10")
        int size,
        @Schema(description = "전체 데이터 개수", example = "42")
        long totalElements,
        @Schema(description = "전체 페이지 수", example = "5")
        int totalPages
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
