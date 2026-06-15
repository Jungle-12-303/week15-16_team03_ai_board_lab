package com.example.aiknowledgeboard.ai.agent;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Agent 실행 단계 응답")
public record AgentStepResponse(
        @Schema(description = "실행한 도구 이름", example = "recommend_tags")
        String tool,
        @Schema(description = "실행 상태", example = "success")
        String status,
        @Schema(description = "단계별 메시지", example = "초안에서 핵심 키워드를 추출했습니다.")
        String message
) {
}
