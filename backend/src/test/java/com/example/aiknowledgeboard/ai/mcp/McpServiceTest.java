package com.example.aiknowledgeboard.ai.mcp;

import com.example.aiknowledgeboard.ai.notion.NotionClient;
import com.example.aiknowledgeboard.ai.notion.NotionClientException;
import com.example.aiknowledgeboard.ai.notion.NotionSaveRequest;
import com.example.aiknowledgeboard.ai.notion.NotionSaveResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class McpServiceTest {
    @Mock
    McpToolLogRepository logRepository;

    @Mock
    NotionClient notionClient;

    McpService mcpService;

    @BeforeEach
    void setUp() {
        mcpService = new McpService("", logRepository, notionClient, new ObjectMapper());
    }

    @Test
    void call_savesRagAnswerToNotion() {
        NotionSaveResponse notionResponse = new NotionSaveResponse("page-id", "https://notion.so/page-id", true);
        when(notionClient.saveRagAnswer(any(NotionSaveRequest.class))).thenReturn(notionResponse);

        JsonRpcResponse response = mcpService.call(new JsonRpcRequest(
                "2.0",
                "notion.saveRagAnswer",
                Map.of(
                        "question", "Spring AI RAG 구조가 뭐야?",
                        "answer", "Spring AI RAG는 검색된 게시글을 근거로 답변합니다.",
                        "sources", List.of(Map.of(
                                "id", 3,
                                "title", "Spring AI RAG 게시판 Q&A 봇 구현 메모",
                                "contentPreview", "게시판 Q&A 봇은 사용자의 질문을 임베딩으로 바꾼 뒤...",
                                "authorNickname", "tester",
                                "score", 0.82,
                                "link", "/posts/3"
                        ))
                ),
                "notion-1"
        ));

        assertThat(response.error()).isNull();
        assertThat(response.result()).isEqualTo(notionResponse);

        ArgumentCaptor<NotionSaveRequest> captor = ArgumentCaptor.forClass(NotionSaveRequest.class);
        verify(notionClient).saveRagAnswer(captor.capture());
        assertThat(captor.getValue().question()).isEqualTo("Spring AI RAG 구조가 뭐야?");
        assertThat(captor.getValue().answer()).isEqualTo("Spring AI RAG는 검색된 게시글을 근거로 답변합니다.");
        assertThat(captor.getValue().sources()).hasSize(1);
    }

    @Test
    void call_returnsErrorForUnknownMethod() {
        JsonRpcResponse response = mcpService.call(new JsonRpcRequest(
                "2.0",
                "unknown.method",
                Map.of(),
                "unknown-1"
        ));

        assertThat(response.result()).isNull();
        assertThat(response.error()).isNotNull();
        assertThat(response.error().message()).contains("지원하지 않는 MCP method");
    }

    @Test
    void call_returnsErrorWhenNotionParamsAreMissing() {
        JsonRpcResponse response = mcpService.call(new JsonRpcRequest(
                "2.0",
                "notion.saveRagAnswer",
                null,
                "notion-empty"
        ));

        assertThat(response.result()).isNull();
        assertThat(response.error()).isNotNull();
        assertThat(response.error().message()).isEqualTo("Notion 저장 실패");
        verify(notionClient, never()).saveRagAnswer(any());
    }

    @Test
    void call_hidesNotionInternalFailureMessage() {
        when(notionClient.saveRagAnswer(any(NotionSaveRequest.class)))
                .thenThrow(new NotionClientException("Notion token rejected"));

        JsonRpcResponse response = mcpService.call(new JsonRpcRequest(
                "2.0",
                "notion.saveRagAnswer",
                Map.of(
                        "question", "RAG 답변 저장해줘",
                        "answer", "저장할 답변입니다.",
                        "sources", List.of()
                ),
                "notion-fail"
        ));

        assertThat(response.result()).isNull();
        assertThat(response.error()).isNotNull();
        assertThat(response.error().message()).isEqualTo("Notion 저장 실패");
    }
}
