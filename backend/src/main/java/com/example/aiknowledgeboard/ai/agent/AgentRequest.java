package com.example.aiknowledgeboard.ai.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "작성 보조 Agent 요청")
public record AgentRequest(
        @Schema(description = "사용자가 작성 중인 글 초안", example = "Spring Security JWT 인증을 정리하고 있습니다.")
        @NotBlank String draft,
        @Schema(description = "원하는 도움 방향", example = "태그 추천과 비슷한 글을 찾아줘")
        String intention,
        @Schema(description = "Agent 메모리를 이어가기 위한 세션 ID", example = "session-123")
        String sessionId
) {
}
