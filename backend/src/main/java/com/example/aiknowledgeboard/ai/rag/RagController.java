package com.example.aiknowledgeboard.ai.rag;

import com.example.aiknowledgeboard.ai.rag2.RagService2;
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
@RequestMapping("/api/ai/rag")
@Tag(name = "AI - RAG", description = "유사 게시글 검색과 요약 API")
@SecurityRequirement(name = "bearerAuth")
public class RagController {
    private final RagService2 ragService;

    public RagController(RagService2 ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/similar")
    @Operation(summary = "유사 게시글 검색과 요약", description = "질문 또는 글 초안을 기준으로 유사 게시글을 찾고 요약과 출처를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "유사 게시글 검색 성공",
                    content = @Content(schema = @Schema(implementation = RagResponse.class))),
            @ApiResponse(responseCode = "400", description = "입력값 검증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    RagResponse similar(@Valid @RequestBody RagRequest request) {
        return ragService.answer(request.query(), request.excludePostId());
    }

    @PostMapping("/chat")
    @Operation(summary = "게시판 지식 Q&A 챗봇", description = "사용자 질문을 기준으로 게시판 벡터 저장소에서 관련 게시글을 찾고 답변과 출처를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "챗봇 답변 생성 성공",
                    content = @Content(schema = @Schema(implementation = RagResponse.class))),
            @ApiResponse(responseCode = "400", description = "입력값 검증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    RagResponse chat(@Valid @RequestBody RagChatRequest request) {
        return ragService.answer(request.message(), null);
    }
}
