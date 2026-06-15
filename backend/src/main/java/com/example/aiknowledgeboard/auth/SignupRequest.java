package com.example.aiknowledgeboard.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "회원가입 요청")
public record SignupRequest(
        @Schema(description = "로그인에 사용할 이메일", example = "user@example.com")
        @Email @NotBlank String email,
        @Schema(description = "화면에 표시할 닉네임", example = "tester", minLength = 2, maxLength = 40)
        @NotBlank @Size(min = 2, max = 40) String nickname,
        @Schema(description = "로그인 비밀번호", example = "password123", minLength = 6, maxLength = 80)
        @NotBlank @Size(min = 6, max = 80) String password
) {
}
