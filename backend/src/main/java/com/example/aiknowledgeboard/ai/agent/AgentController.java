package com.example.aiknowledgeboard.ai.agent;

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
@RequestMapping("/api/ai/agent")
@Tag(name = "AI - Agent", description = "게시글 작성 보조 Agent API")
@SecurityRequirement(name = "bearerAuth")
public class AgentController {
    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/write-helper")
    @Operation(summary = "작성 보조 Agent 실행", description = "글 초안을 분석해 태그 추천, 유사 글 검색, 외부 도구 호출 결과를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agent 실행 성공",
                    content = @Content(schema = @Schema(implementation = AgentResponse.class))),
            @ApiResponse(responseCode = "400", description = "입력값 검증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    AgentResponse help(@Valid @RequestBody AgentRequest request) {
        return agentService.help(request);
    }
}
