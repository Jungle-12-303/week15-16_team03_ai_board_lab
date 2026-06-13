package com.jungle_choi.namanmu.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class McpServerService {

    private static final String JSON_RPC_VERSION = "2.0";
    private static final String WEATHER_TOOL_NAME = "weather.current_forecast";
    private static final String GITHUB_REPOSITORY_TOOL_NAME = "github.repository_summary";

    private final WeatherApiClient weatherApiClient;
    private final GitHubApiClient gitHubApiClient;
    private final ObjectMapper objectMapper;

    public McpServerService(
            WeatherApiClient weatherApiClient,
            GitHubApiClient gitHubApiClient,
            ObjectMapper objectMapper) {
        this.weatherApiClient = weatherApiClient;
        this.gitHubApiClient = gitHubApiClient;
        this.objectMapper = objectMapper;
    }

    public McpJsonRpcResponse handle(McpJsonRpcRequest request) {
        if (!JSON_RPC_VERSION.equals(request.jsonrpc())) {
            return error(request.id(), -32600, "jsonrpc must be 2.0.");
        }

        return switch (request.method()) {
            case "tools/list" -> ok(request.id(), listTools());
            case "tools/call" -> callTool(request);
            default -> error(request.id(), -32601, "Unsupported MCP method.");
        };
    }

    private McpJsonRpcResponse callTool(McpJsonRpcRequest request) {
        JsonNode params = request.params();
        if (params == null || params.isMissingNode()) {
            return error(request.id(), -32602, "tools/call params are required.");
        }

        String toolName = params.path("name").asText("");
        return switch (toolName) {
            case WEATHER_TOOL_NAME -> callWeatherTool(request.id(), params.path("arguments"));
            case GITHUB_REPOSITORY_TOOL_NAME -> callGitHubRepositoryTool(request.id(), params.path("arguments"));
            default -> error(request.id(), -32602, "Unsupported MCP tool.");
        };
    }

    private McpJsonRpcResponse callWeatherTool(JsonNode id, JsonNode arguments) {
        String location = arguments.path("location").asText("서울");
        WeatherApiClient.WeatherReport weatherReport = weatherApiClient.getCurrentForecast(location);

        return ok(id, new McpToolCallResult(
                false,
                List.of(new McpContent("text", weatherReport.toBriefingText())),
                weatherReport));
    }

    private McpJsonRpcResponse callGitHubRepositoryTool(JsonNode id, JsonNode arguments) {
        String owner = arguments.path("owner").asText("");
        String repo = arguments.path("repo").asText("");

        if (owner.isBlank() || repo.isBlank()) {
            return error(id, -32602, "owner and repo are required.");
        }

        GitHubApiClient.GitHubRepositoryReport repositoryReport =
                gitHubApiClient.getRepository(owner, repo);

        return ok(id, new McpToolCallResult(
                false,
                List.of(new McpContent("text", repositoryReport.toFactText())),
                repositoryReport));
    }

    private Object listTools() {
        return Map.of(
                "tools",
                List.of(
                        Map.of(
                                "name", WEATHER_TOOL_NAME,
                                "description", "작성 중인 글에 필요한 지역의 현재 날씨와 오늘 예보를 조회합니다.",
                                "inputSchema", Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "location", Map.of(
                                                        "type", "string",
                                                        "description", "날씨를 조회할 지역명. 예: 서울, 부산, 제주")),
                                        "required", List.of("location"))),
                        Map.of(
                                "name", GITHUB_REPOSITORY_TOOL_NAME,
                                "description", "GitHub 공개 저장소의 star, fork, issue, 언어, 라이선스 등 메타데이터를 조회합니다.",
                                "inputSchema", Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "owner", Map.of(
                                                        "type", "string",
                                                        "description", "GitHub 저장소 소유자. 예: facebook"),
                                                "repo", Map.of(
                                                        "type", "string",
                                                        "description", "GitHub 저장소 이름. 예: react")),
                                        "required", List.of("owner", "repo")))));
    }

    public McpToolCallResult callWeatherTool(String location) {
        JsonNode params = objectMapper.valueToTree(Map.of(
                "name", WEATHER_TOOL_NAME,
                "arguments", Map.of("location", location)));
        McpJsonRpcResponse response = handle(new McpJsonRpcRequest(
                JSON_RPC_VERSION,
                "tools/call",
                params,
                objectMapper.valueToTree("weather-fact-check")));

        if (response.error() != null) {
            throw new IllegalStateException(response.error().message());
        }

        return objectMapper.convertValue(response.result(), McpToolCallResult.class);
    }

    public McpToolCallResult callGitHubRepositoryTool(String owner, String repo) {
        JsonNode params = objectMapper.valueToTree(Map.of(
                "name", GITHUB_REPOSITORY_TOOL_NAME,
                "arguments", Map.of(
                        "owner", owner,
                        "repo", repo)));
        McpJsonRpcResponse response = handle(new McpJsonRpcRequest(
                JSON_RPC_VERSION,
                "tools/call",
                params,
                objectMapper.valueToTree("github-fact-check")));

        if (response.error() != null) {
            throw new IllegalStateException(response.error().message());
        }

        return objectMapper.convertValue(response.result(), McpToolCallResult.class);
    }

    private static McpJsonRpcResponse ok(JsonNode id, Object result) {
        return new McpJsonRpcResponse(JSON_RPC_VERSION, result, null, id);
    }

    private static McpJsonRpcResponse error(JsonNode id, int code, String message) {
        return new McpJsonRpcResponse(
                JSON_RPC_VERSION,
                null,
                new McpJsonRpcError(code, message),
                id);
    }

    public record McpJsonRpcRequest(
            String jsonrpc,
            String method,
            JsonNode params,
            JsonNode id) {
    }

    public record McpJsonRpcResponse(
            String jsonrpc,
            Object result,
            McpJsonRpcError error,
            JsonNode id) {
    }

    public record McpJsonRpcError(int code, String message) {
    }

    public record McpToolCallResult(
            boolean isError,
            List<McpContent> content,
            Object structuredContent) {
    }

    public record McpContent(String type, String text) {
    }
}
