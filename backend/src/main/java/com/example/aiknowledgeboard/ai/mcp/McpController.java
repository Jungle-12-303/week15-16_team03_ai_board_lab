package com.example.aiknowledgeboard.ai.mcp;

import com.example.aiknowledgeboard.common.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/mcp")
@Tag(name = "AI - MCP", description = "JSON-RPC 스타일 외부 도구 호출 API")
@SecurityRequirement(name = "bearerAuth")
public class McpController {
    private final McpService mcpService;

    public McpController(McpService mcpService) {
        this.mcpService = mcpService;
    }

    @PostMapping("/call")
    @Operation(summary = "MCP 도구 호출", description = "JSON-RPC 형식으로 GitHub 사용자 또는 저장소 조회 도구를 호출합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "MCP 호출 처리 완료",
                    content = @Content(schema = @Schema(implementation = JsonRpcResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 JSON-RPC 요청",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    JsonRpcResponse call(@RequestBody JsonRpcRequest request) {
        return mcpService.call(request);
    }

    /*
    @PostMapping("/mcp/weather")
    McpResponse weather(@Valid RequestBody McpRequest reqeust) {
        return mcpService.answer(request.message);
    }

     */

}

