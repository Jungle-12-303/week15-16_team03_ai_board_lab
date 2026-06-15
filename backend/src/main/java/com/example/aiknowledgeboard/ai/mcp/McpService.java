package com.example.aiknowledgeboard.ai.mcp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class McpService {
    private final String githubToken;
    private final RestClient githubClient;
    private final McpToolLogRepository logRepository;

    public McpService(@Value("${github.token}") String githubToken, McpToolLogRepository logRepository) {
        this.githubToken = githubToken;
        this.logRepository = logRepository;
        this.githubClient = RestClient.builder().baseUrl("https://api.github.com").build();
    }

    public JsonRpcResponse call(JsonRpcRequest request) {
        if (request == null || !"2.0".equals(request.jsonrpc())) {
            return JsonRpcResponse.error(-32600, "jsonrpc 값은 2.0이어야 합니다.", request == null ? null : request.id());
        }
        try {
            Object result = switch (request.method()) {
                case "github.getUser" -> getGithubUser(requiredParam(request, "username"));
                case "github.getRepo" -> getGithubRepo(requiredParam(request, "owner"), requiredParam(request, "repo"));
                default -> throw new IllegalArgumentException("지원하지 않는 MCP method 입니다: " + request.method());
            };
            saveLog(request.method(), request.params().toString(), result.toString(), "SUCCESS", null);
            return JsonRpcResponse.success(result, request.id());
        } catch (Exception ex) {
            saveLog(request.method(), request.params() == null ? null : request.params().toString(), null, "FAIL", ex.getMessage());
            return JsonRpcResponse.error(-32000, ex.getMessage(), request.id());
        }
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

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }
}
