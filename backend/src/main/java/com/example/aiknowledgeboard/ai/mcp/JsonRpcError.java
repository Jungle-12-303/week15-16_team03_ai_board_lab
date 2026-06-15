package com.example.aiknowledgeboard.ai.mcp;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "JSON-RPC 오류 정보")
public record JsonRpcError(
        @Schema(description = "JSON-RPC 오류 코드", example = "-32601")
        int code,
        @Schema(description = "오류 메시지", example = "지원하지 않는 method입니다.")
        String message
) {
}
