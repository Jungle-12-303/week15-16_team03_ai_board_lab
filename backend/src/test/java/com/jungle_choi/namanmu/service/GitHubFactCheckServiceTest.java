package com.jungle_choi.namanmu.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class GitHubFactCheckServiceTest {

    private final McpServerService mcpServerService = Mockito.mock(McpServerService.class);
    private final OpenAiTextClient openAiTextClient = Mockito.mock(OpenAiTextClient.class);
    private final GitHubFactCheckService gitHubFactCheckService = new GitHubFactCheckService(
            mcpServerService,
            openAiTextClient,
            new ObjectMapper());

    @Test
    void checkDoesNotCallToolWhenRepositoryReferenceIsMissing() {
        GitHubFactCheckService.GitHubFactCheckResult result = gitHubFactCheckService.check(
                "Learning",
                "React 상태 관리 메모",
                "useState와 custom hook을 정리합니다.",
                List.of("React"));

        assertThat(result.status()).isEqualTo("NOT_SUPPORTED");
        verify(mcpServerService, never()).callGitHubRepositoryTool(anyString(), anyString());
    }

    @Test
    void checkCallsGitHubToolWhenRepositoryUrlExists() {
        GitHubApiClient.GitHubRepositoryReport repositoryReport = repositoryReport();
        when(mcpServerService.callGitHubRepositoryTool("facebook", "react"))
                .thenReturn(new McpServerService.McpToolCallResult(
                        false,
                        List.of(),
                        repositoryReport));
        when(openAiTextClient.generateStructuredJson(anyString(), anyString(), any()))
                .thenReturn(new OpenAiTextClient.TextGenerationResult("""
                        {
                          "claim": "facebook/react는 Star가 10만개 이상이다",
                          "verdict": "supported",
                          "comparison": "외부 정보의 Star 수가 230000이므로 10만개 이상이라는 표현은 맞다.",
                          "suggestion": "Star가 약 23만개라고 더 구체적으로 쓰는 것이 좋습니다.",
                          "summary": "GitHub 저장소 수치가 게시글의 표현을 뒷받침합니다."
                        }
                        """));

        GitHubFactCheckService.GitHubFactCheckResult result = gitHubFactCheckService.check(
                "Learning",
                "facebook/react GitHub 메모",
                "https://github.com/facebook/react 는 Star가 10만개 이상인 React 저장소입니다.",
                List.of("GitHub", "React"));

        assertThat(result.status()).isEqualTo("CHECKED");
        assertThat(result.repository()).isEqualTo("facebook/react");
        assertThat(result.verdict()).isEqualTo("supported");
        assertThat(result.comparison()).contains("230000");
        assertThat(result.suggestion()).contains("23만");
        verify(mcpServerService).callGitHubRepositoryTool("facebook", "react");
        ArgumentCaptor<OpenAiTextClient.StructuredJsonSchema> schemaCaptor =
                ArgumentCaptor.forClass(OpenAiTextClient.StructuredJsonSchema.class);
        verify(openAiTextClient).generateStructuredJson(anyString(), anyString(), schemaCaptor.capture());
        assertThat(schemaCaptor.getValue().name()).isEqualTo("github_repository_fact_check");
    }

    @Test
    void checkReturnsToolErrorWhenGitHubToolFails() {
        when(mcpServerService.callGitHubRepositoryTool("missing", "repo"))
                .thenThrow(new IllegalStateException("not found"));

        GitHubFactCheckService.GitHubFactCheckResult result = gitHubFactCheckService.check(
                "Learning",
                "GitHub 저장소 확인",
                "https://github.com/missing/repo 저장소 정보를 확인합니다.",
                List.of("GitHub"));

        assertThat(result.status()).isEqualTo("TOOL_ERROR");
        assertThat(result.repository()).isEqualTo("missing/repo");
        verify(openAiTextClient, never()).generateStructuredJson(anyString(), anyString(), any());
    }

    @Test
    void extractRepositoryReferenceRequiresGitHubIntentForOwnerSlashRepoText() {
        assertThat(GitHubFactCheckService.extractRepositoryReference("facebook/react를 봤다"))
                .isEmpty();
        assertThat(GitHubFactCheckService.extractRepositoryReference("GitHub facebook/react를 봤다"))
                .contains(new GitHubFactCheckService.RepositoryReference("facebook", "react"));
    }

    @Test
    void extractRepositoryReferenceTrimsGitSuffixFromRepositoryUrl() {
        assertThat(GitHubFactCheckService.extractRepositoryReference("https://github.com/facebook/react.git"))
                .contains(new GitHubFactCheckService.RepositoryReference("facebook", "react"));
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
