package com.jungle_choi.namanmu.service;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class McpFactCheckService {

    private final GitHubFactCheckService gitHubFactCheckService;
    private final WeatherFactCheckService weatherFactCheckService;

    public McpFactCheckService(
            GitHubFactCheckService gitHubFactCheckService,
            WeatherFactCheckService weatherFactCheckService) {
        this.gitHubFactCheckService = gitHubFactCheckService;
        this.weatherFactCheckService = weatherFactCheckService;
    }

    public UnifiedFactCheckResult check(
            String category,
            String title,
            String content,
            List<String> tags) {
        GitHubFactCheckService.GitHubFactCheckResult gitHubResult =
                gitHubFactCheckService.check(category, title, content, tags);
        if (!"NOT_SUPPORTED".equals(gitHubResult.status())) {
            return UnifiedFactCheckResult.from(gitHubResult);
        }

        WeatherFactCheckService.WeatherFactCheckResult weatherResult =
                weatherFactCheckService.check(category, title, content, tags);
        if (!"NOT_SUPPORTED".equals(weatherResult.status())) {
            return UnifiedFactCheckResult.from(weatherResult);
        }

        return new UnifiedFactCheckResult(
                "NOT_SUPPORTED",
                "이 게시글에서 MCP로 확인할 GitHub 저장소나 날씨 관련 주장을 찾지 못했습니다.",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "");
    }

    public record UnifiedFactCheckResult(
            String status,
            String message,
            String toolName,
            String location,
            String repository,
            String repositoryUrl,
            String source,
            String observedAt,
            String externalFact,
            String claim,
            String verdict,
            String comparison,
            String suggestion,
            String judgement) {

        static UnifiedFactCheckResult from(GitHubFactCheckService.GitHubFactCheckResult result) {
            return new UnifiedFactCheckResult(
                    result.status(),
                    result.message(),
                    result.toolName(),
                    "",
                    result.repository(),
                    result.repositoryUrl(),
                    result.source(),
                    result.observedAt(),
                    result.externalFact(),
                    result.claim(),
                    result.verdict(),
                    result.comparison(),
                    result.suggestion(),
                    result.judgement());
        }

        static UnifiedFactCheckResult from(WeatherFactCheckService.WeatherFactCheckResult result) {
            return new UnifiedFactCheckResult(
                    result.status(),
                    result.message(),
                    result.toolName(),
                    result.location(),
                    "",
                    "",
                    result.source(),
                    result.observedAt(),
                    result.externalFact(),
                    result.claim(),
                    result.verdict(),
                    result.comparison(),
                    result.suggestion(),
                    result.judgement());
        }
    }
}
