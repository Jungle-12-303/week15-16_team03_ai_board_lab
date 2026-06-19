package com.example.aiknowledgeboard.ai.mcp;

import com.example.aiknowledgeboard.ai.notion.NotionClient;
import com.example.aiknowledgeboard.ai.notion.NotionSaveRequest;
import com.example.aiknowledgeboard.ai.notion.NotionSaveResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class McpService {
    private static final String NOTION_SAVE_RAG_ANSWER = "notion.saveRagAnswer";

    private final String githubToken;
    private final RestClient githubClient;
    private final McpToolLogRepository logRepository;
    private final NotionClient notionClient;
    private final ObjectMapper objectMapper;

    public McpService(
            @Value("${github.token}") String githubToken,
            McpToolLogRepository logRepository,
            NotionClient notionClient,
            ObjectMapper objectMapper
    ) {
        this.githubToken = githubToken;
        this.logRepository = logRepository;
        this.notionClient = notionClient;
        this.objectMapper = objectMapper;
        this.githubClient = RestClient.builder().baseUrl("https://api.github.com").build();
    }

    public JsonRpcResponse call(JsonRpcRequest request) {
        if (request == null || !"2.0".equals(request.jsonrpc())) {
            return JsonRpcResponse.error(-32600, "jsonrpc 값은 2.0이어야 합니다.", request == null ? null : request.id());
        }
        try {
            ToolExecution execution = execute(request);
            saveLog(request.method(), execution.requestSummary(), execution.responseSummary(), "SUCCESS", null);
            return JsonRpcResponse.success(execution.result(), request.id());
        } catch (Exception ex) {
            saveLog(request.method(), requestSummary(request), null, "FAIL", ex.getMessage());
            String message = NOTION_SAVE_RAG_ANSWER.equals(request.method()) ? "Notion 저장 실패" : ex.getMessage();
            return JsonRpcResponse.error(-32000, message, request.id());
        }
    }

    private ToolExecution execute(JsonRpcRequest request) {
        return switch (request.method()) {
            case "github.getUser" -> {
                Map<String, Object> result = getGithubUser(requiredParam(request, "username"));
                yield new ToolExecution(result, requestSummary(request), result.toString());
            }
            case "github.getRepo" -> {
                Map<String, Object> result = getGithubRepo(requiredParam(request, "owner"), requiredParam(request, "repo"));
                yield new ToolExecution(result, requestSummary(request), result.toString());
            }
            case NOTION_SAVE_RAG_ANSWER -> {
                NotionSaveRequest notionRequest = toNotionSaveRequest(request);
                NotionSaveResponse response = notionClient.saveRagAnswer(notionRequest);
                yield new ToolExecution(response, notionRequestSummary(notionRequest), notionResponseSummary(response));
            }
            default -> throw new IllegalArgumentException("지원하지 않는 MCP method 입니다: " + request.method());
        };
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveLog(String toolName, String requestSummary, String responseSummary, String status, String errorMessage) {
        logRepository.save(new McpToolLog(
                toolName == null ? "unknown" : toolName,
                trim(requestSummary),
                trim(responseSummary),
                status,
                trim(errorMessage)
        ));
    }

    private Map<String, Object> getGithubUser(String username) {
        Map<?, ?> response = githubGet("/users/" + username);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("login", response.get("login"));
        result.put("name", response.get("name"));
        result.put("publicRepos", response.get("public_repos"));
        result.put("followers", response.get("followers"));
        result.put("profileUrl", response.get("html_url"));
        return result;
    }

    private Map<String, Object> getGithubRepo(String owner, String repo) {
        Map<?, ?> response = githubGet("/repos/" + owner + "/" + repo);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fullName", response.get("full_name"));
        result.put("description", response.get("description"));
        result.put("stars", response.get("stargazers_count"));
        result.put("forks", response.get("forks_count"));
        result.put("language", response.get("language"));
        result.put("repoUrl", response.get("html_url"));
        return result;
    }

    private Map<?, ?> githubGet(String path) {
        return githubClient.get()
                .uri(path)
                .headers(headers -> {
                    headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
                    if (githubToken != null && !githubToken.isBlank()) {
                        headers.setBearerAuth(githubToken);
                    }
                })
                .retrieve()
                .body(Map.class);
    }

    private String requiredParam(JsonRpcRequest request, String name) {
        if (request.params() == null || request.params().get(name) == null || request.params().get(name).toString().isBlank()) {
            throw new IllegalArgumentException(name + " 파라미터가 필요합니다.");
        }
        return request.params().get(name).toString().trim();
    }

    private NotionSaveRequest toNotionSaveRequest(JsonRpcRequest request) {
        if (request.params() == null || request.params().isEmpty()) {
            throw new IllegalArgumentException("Notion 저장 params가 필요합니다.");
        }
        NotionSaveRequest notionRequest = objectMapper.convertValue(request.params(), NotionSaveRequest.class);
        if (notionRequest == null || isBlank(notionRequest.question()) || isBlank(notionRequest.answer())) {
            throw new IllegalArgumentException("question과 answer가 필요합니다.");
        }
        if (notionRequest.sources() == null) {
            return new NotionSaveRequest(notionRequest.question(), notionRequest.answer(), List.of());
        }
        return notionRequest;
    }

    private String requestSummary(JsonRpcRequest request) {
        if (request.params() == null) {
            return null;
        }
        if (NOTION_SAVE_RAG_ANSWER.equals(request.method())) {
            return notionRequestSummary(request.params());
        }
        return request.params().toString();
    }

    private String notionRequestSummary(NotionSaveRequest request) {
        return "question=" + shorten(request.question(), 200) + ", sourcesCount=" + request.sources().size();
    }

    private String notionRequestSummary(Map<String, Object> params) {
        Object question = params.get("question");
        Object sources = params.get("sources");
        int sourcesCount = sources instanceof List<?> list ? list.size() : 0;
        return "question=" + shorten(question == null ? null : question.toString(), 200) + ", sourcesCount=" + sourcesCount;
    }

    private String notionResponseSummary(NotionSaveResponse response) {
        return "saved=" + response.saved() + ", notionPageId=" + response.notionPageId() + ", notionUrl=" + response.notionUrl();
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }

    private String shorten(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record ToolExecution(Object result, String requestSummary, String responseSummary) {
    }
}
