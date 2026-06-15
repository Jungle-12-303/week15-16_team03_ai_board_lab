package com.example.aiknowledgeboard.common;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "공통 오류 응답")
public record ErrorResponse(
        @Schema(description = "애플리케이션 오류 코드", example = "BAD_REQUEST")
        String code,
        @Schema(description = "오류 메시지", example = "이미 가입된 이메일입니다.")
        String message,
        @Schema(description = "오류 발생 시각", example = "2026-06-12T10:15:30.123Z")
        Instant timestamp
) {
    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, Instant.now());
    }
}
