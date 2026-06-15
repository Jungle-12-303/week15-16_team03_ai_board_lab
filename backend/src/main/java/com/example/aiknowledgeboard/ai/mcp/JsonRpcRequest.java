package com.example.aiknowledgeboard.ai.mcp;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "JSON-RPC 스타일 MCP 요청")
public record JsonRpcRequest(
        @Schema(description = "JSON-RPC 버전", example = "2.0")
        String jsonrpc,
        @Schema(description = "호출할 MCP 도구 이름", example = "github.getUser", allowableValues = {"github.getUser", "github.getRepo"})
        String method,
        @Schema(description = "도구별 파라미터. github.getUser는 username, github.getRepo는 owner와 repo를 사용합니다.")
        Map<String, Object> params,
        @Schema(description = "요청-응답 매칭용 ID", example = "req-1")
        String id
) {
}
