package com.example.aiknowledgeboard.ai.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "JSON-RPC 스타일 MCP 응답")
public record JsonRpcResponse(
        @Schema(description = "JSON-RPC 버전", example = "2.0")
        String jsonrpc,
        @Schema(description = "도구 호출 성공 결과. method에 따라 구조가 달라집니다.")
        Object result,
        @Schema(description = "도구 호출 실패 정보")
        JsonRpcError error,
        @Schema(description = "요청에서 받은 ID", example = "req-1")
        String id
) {
    public static JsonRpcResponse success(Object result, String id) {
        return new JsonRpcResponse("2.0", result, null, id);
    }

    public static JsonRpcResponse error(int code, String message, String id) {
        return new JsonRpcResponse("2.0", null, new JsonRpcError(code, message), id);
    }
}
