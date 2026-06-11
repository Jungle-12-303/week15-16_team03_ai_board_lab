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

    private final WeatherApiClient weatherApiClient;
    private final ObjectMapper objectMapper;

    public McpServerService(
            WeatherApiClient weatherApiClient,
            ObjectMapper objectMapper) {
        this.weatherApiClient = weatherApiClient;
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
        if (!WEATHER_TOOL_NAME.equals(toolName)) {
            return error(request.id(), -32602, "Unsupported MCP tool.");
        }

        String location = params.path("arguments").path("location").asText("서울");
        WeatherApiClient.WeatherReport weatherReport =
                weatherApiClient.getCurrentForecast(location);

        return ok(request.id(), new McpToolCallResult(
                false,
                List.of(new McpContent("text", weatherReport.toBriefingText())),
                weatherReport));
    }

    private Object listTools() {
        return Map.of(
                "tools",
                List.of(Map.of(
                        "name", WEATHER_TOOL_NAME,
                        "description", "작성 중인 글에 필요한 지역의 현재 날씨와 오늘 예보를 조회합니다.",
                        "inputSchema", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "location", Map.of(
                                                "type", "string",
                                                "description", "날씨를 조회할 지역명. 예: 서울, 부산, 제주")),
                                "required", List.of("location")))));
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
