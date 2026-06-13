package com.jungle_choi.namanmu.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class McpServerServiceTest {

    private final WeatherApiClient weatherApiClient = Mockito.mock(WeatherApiClient.class);
    private final GitHubApiClient gitHubApiClient = Mockito.mock(GitHubApiClient.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final McpServerService mcpServerService =
            new McpServerService(weatherApiClient, gitHubApiClient, objectMapper);

    @Test
    void listToolsIncludesWeatherAndGitHubRepositoryTools() {
        McpServerService.McpJsonRpcResponse response = mcpServerService.handle(
                new McpServerService.McpJsonRpcRequest(
                        "2.0",
                        "tools/list",
                        null,
                        objectMapper.valueToTree("tools-list")));

        JsonNode result = objectMapper.valueToTree(response.result());

        assertThat(result.path("tools"))
                .extracting((tool) -> tool.path("name").asText())
                .contains("weather.current_forecast", "github.repository_summary");
    }

    @Test
    void handleRejectsMissingMethodAsJsonRpcError() {
        McpServerService.McpJsonRpcResponse response = mcpServerService.handle(
                new McpServerService.McpJsonRpcRequest(
                        "2.0",
                        null,
                        null,
                        objectMapper.valueToTree("missing-method")));

        assertThat(response.error()).isNotNull();
        assertThat(response.error().code()).isEqualTo(-32600);
        assertThat(response.error().message()).contains("method");
    }

    @Test
    void handleRejectsNullRequestAsJsonRpcError() {
        McpServerService.McpJsonRpcResponse response = mcpServerService.handle(null);

        assertThat(response.error()).isNotNull();
        assertThat(response.error().code()).isEqualTo(-32600);
        assertThat(response.error().message()).contains("request");
    }

    @Test
    void callGitHubRepositoryToolReturnsRepositoryMetadata() {
        GitHubApiClient.GitHubRepositoryReport repositoryReport = repositoryReport();
        when(gitHubApiClient.getRepository("facebook", "react"))
                .thenReturn(repositoryReport);

        McpServerService.McpToolCallResult result =
                mcpServerService.callGitHubRepositoryTool("facebook", "react");

        assertThat(result.isError()).isFalse();
        assertThat(result.content())
                .extracting(McpServerService.McpContent::text)
                .first()
                .asString()
                .contains("facebook/react", "Star: 230000");
    }

    @Test
    void callToolRejectsGitHubRepositoryRequestWithoutOwner() {
        McpServerService.McpJsonRpcResponse response = mcpServerService.handle(
                new McpServerService.McpJsonRpcRequest(
                        "2.0",
                        "tools/call",
                        objectMapper.valueToTree(Map.of(
                                "name", "github.repository_summary",
                                "arguments", Map.of("repo", "react"))),
                        objectMapper.valueToTree("github-tool")));

        assertThat(response.error()).isNotNull();
        assertThat(response.error().message()).contains("owner and repo");
    }

    private static GitHubApiClient.GitHubRepositoryReport repositoryReport() {
        return new GitHubApiClient.GitHubRepositoryReport(
                "facebook/react",
                "https://github.com/facebook/react",
                "The library for web and native user interfaces.",
                230000,
                47000,
                1200,
                "JavaScript",
                "main",
                false,
                false,
                "public",
                "MIT License",
                "MIT",
                "2013-05-24T16:15:54Z",
                "2026-06-12T10:00:00Z",
                "2026-06-12T09:30:00Z",
                "GitHub REST API");
    }
}
