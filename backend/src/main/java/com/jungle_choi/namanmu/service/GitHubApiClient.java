package com.jungle_choi.namanmu.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class GitHubApiClient {

    private static final String SOURCE_NAME = "GitHub REST API";
    private static final String GITHUB_API_VERSION = "2022-11-28";
    private static final String USER_AGENT = "project-alpha-ai-board";

    private final RestClient restClient;
    private final String apiBaseUrl;
    private final String token;

    public GitHubApiClient(
            RestClient.Builder restClientBuilder,
            @Value("${app.github.api-base-url:https://api.github.com}") String apiBaseUrl,
            @Value("${app.github.token:}") String token) {
        this.restClient = restClientBuilder.build();
        this.apiBaseUrl = apiBaseUrl;
        this.token = token;
    }

    public GitHubRepositoryReport getRepository(String owner, String repo) {
        String repositoryUrl = UriComponentsBuilder.fromUriString(apiBaseUrl)
                .pathSegment("repos", owner, repo)
                .toUriString();
        RestClient.RequestHeadersSpec<?> request = restClient.get()
                .uri(repositoryUrl)
                .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .header("X-GitHub-Api-Version", GITHUB_API_VERSION)
                .header(HttpHeaders.USER_AGENT, USER_AGENT);

        if (token != null && !token.isBlank()) {
            request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token.trim());
        }

        GitHubRepositoryResponse response = request.retrieve()
                .body(GitHubRepositoryResponse.class);

        if (response == null || response.fullName() == null || response.fullName().isBlank()) {
            throw new IllegalStateException("GitHub repository response is empty.");
        }

        return GitHubRepositoryReport.from(response);
    }

    private record GitHubRepositoryResponse(
            @JsonProperty("full_name") String fullName,
            @JsonProperty("html_url") String htmlUrl,
            String description,
            @JsonProperty("stargazers_count") Integer stargazersCount,
            @JsonProperty("forks_count") Integer forksCount,
            @JsonProperty("open_issues_count") Integer openIssuesCount,
            String language,
            @JsonProperty("default_branch") String defaultBranch,
            Boolean archived,
            Boolean disabled,
            String visibility,
            @JsonProperty("created_at") String createdAt,
            @JsonProperty("updated_at") String updatedAt,
            @JsonProperty("pushed_at") String pushedAt,
            GitHubLicense license) {
    }

    private record GitHubLicense(
            String name,
            @JsonProperty("spdx_id") String spdxId) {
    }

    public record GitHubRepositoryReport(
            String fullName,
            String htmlUrl,
            String description,
            Integer stars,
            Integer forks,
            Integer openIssues,
            String language,
            String defaultBranch,
            Boolean archived,
            Boolean disabled,
            String visibility,
            String licenseName,
            String licenseSpdxId,
            String createdAt,
            String updatedAt,
            String pushedAt,
            String source) {

        private static GitHubRepositoryReport from(GitHubRepositoryResponse response) {
            GitHubLicense license = response.license();

            return new GitHubRepositoryReport(
                    response.fullName(),
                    response.htmlUrl(),
                    response.description(),
                    response.stargazersCount(),
                    response.forksCount(),
                    response.openIssuesCount(),
                    response.language(),
                    response.defaultBranch(),
                    response.archived(),
                    response.disabled(),
                    response.visibility(),
                    license == null ? "" : license.name(),
                    license == null ? "" : license.spdxId(),
                    response.createdAt(),
                    response.updatedAt(),
                    response.pushedAt(),
                    SOURCE_NAME);
        }

        public String toFactText() {
            return """
                    저장소: %s
                    URL: %s
                    설명: %s
                    주 언어: %s
                    Star: %d
                    Fork: %d
                    Open issues: %d
                    기본 브랜치: %s
                    공개 범위: %s
                    Archived: %s
                    Disabled: %s
                    License: %s (%s)
                    생성일: %s
                    수정일: %s
                    최근 push: %s
                    출처: %s
                    """.formatted(
                    valueOrBlank(fullName),
                    valueOrBlank(htmlUrl),
                    valueOrBlank(description),
                    valueOrBlank(language),
                    valueOrZero(stars),
                    valueOrZero(forks),
                    valueOrZero(openIssues),
                    valueOrBlank(defaultBranch),
                    valueOrBlank(visibility),
                    valueOrFalse(archived),
                    valueOrFalse(disabled),
                    valueOrBlank(licenseName),
                    valueOrBlank(licenseSpdxId),
                    valueOrBlank(createdAt),
                    valueOrBlank(updatedAt),
                    valueOrBlank(pushedAt),
                    valueOrBlank(source));
        }

        private static String valueOrBlank(String value) {
            return value == null || value.isBlank() ? "알 수 없음" : value;
        }

        private static int valueOrZero(Integer value) {
            return value == null ? 0 : value;
        }

        private static boolean valueOrFalse(Boolean value) {
            return value != null && value;
        }
    }
}
