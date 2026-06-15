package com.example.aiknowledgeboard.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "인증 성공 응답")
public record AuthResponse(
        @Schema(description = "API 인증에 사용할 JWT", example = "eyJhbGciOiJIUzI1NiJ9...")
        String token,
        @Schema(description = "사용자 ID", example = "1")
        Long userId,
        @Schema(description = "사용자 이메일", example = "user@example.com")
        String email,
        @Schema(description = "사용자 닉네임", example = "tester")
        String nickname
) {
}
