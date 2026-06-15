package com.example.aiknowledgeboard.ai.agent;

import com.example.aiknowledgeboard.ai.rag.SimilarPostResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "작성 보조 Agent 응답")
public record AgentResponse(
        @Schema(description = "사용자에게 보여줄 최종 안내 메시지", example = "초안에는 spring, security, jwt 태그가 적절합니다.")
        String finalMessage,
        @Schema(description = "추천 태그 목록", example = "[\"spring\", \"security\", \"jwt\"]")
        List<String> recommendedTags,
        @Schema(description = "RAG로 찾은 유사 게시글 목록")
        List<SimilarPostResponse> similarPosts,
        @Schema(description = "Agent가 외부 MCP 도구를 호출한 경우의 결과")
        Object mcpResult,
        @Schema(description = "Agent가 실행한 단계 목록")
        List<AgentStepResponse> steps,
        @Schema(description = "OpenAI API 대신 로컬 fallback을 사용했는지 여부", example = "false")
        boolean fallback
) {
}
