package com.jungle_choi.namanmu.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class GitHubFactCheckService {

    private static final String GITHUB_TOOL_NAME = "github.repository_summary";
    private static final Pattern GITHUB_URL_PATTERN = Pattern.compile(
            "(?:https?://)?github\\.com/([A-Za-z0-9_.-]+)/([A-Za-z0-9_.-]+)");
    private static final Pattern OWNER_REPO_PATTERN = Pattern.compile(
            "\\b([A-Za-z0-9_.-]+)/([A-Za-z0-9_.-]+)\\b");
    private static final Pattern REPOSITORY_NAME_TRIM_PATTERN = Pattern.compile("[).,;:!?]+$");
    private static final OpenAiTextClient.StructuredJsonSchema GITHUB_FACT_CHECK_SCHEMA =
            new OpenAiTextClient.StructuredJsonSchema(
                    "github_repository_fact_check",
                    "Structured comparison between a post's GitHub repository claim and fetched repository metadata.",
                    Map.of(
                            "type", "object",
                            "additionalProperties", false,
                            "properties", Map.of(
                                    "claim", Map.of(
                                            "type", "string",
                                            "description", "The exact GitHub repository-related claim from the post."),
                                    "verdict", Map.of(
                                            "type", "string",
                                            "enum", List.of("supported", "contradicted", "uncertain", "too_vague")),
                                    "comparison", Map.of(
                                            "type", "string",
                                            "description", "How the post claim matches or differs from repository metadata."),
                                    "suggestion", Map.of(
                                            "type", "string",
                                            "description", "Safer wording if the post should be revised."),
                                    "summary", Map.of(
                                            "type", "string",
                                            "description", "One short Korean sentence for the UI.")),
                            "required", List.of("claim", "verdict", "comparison", "suggestion", "summary")));
    private static final String FACT_CHECK_INSTRUCTIONS = """
            You are a careful fact checker for Project Alpha posts.
            Use only the given GitHub repository metadata and the post content.
            Check only claims about the referenced GitHub repository.
            Do not judge claims that are not covered by the fetched metadata.
            Write in Korean.
            Keep the result concise and practical.
            """;

    private final McpServerService mcpServerService;
    private final OpenAiTextClient openAiTextClient;
    private final ObjectMapper objectMapper;

    public GitHubFactCheckService(
            McpServerService mcpServerService,
            OpenAiTextClient openAiTextClient,
            ObjectMapper objectMapper) {
        this.mcpServerService = mcpServerService;
        this.openAiTextClient = openAiTextClient;
        this.objectMapper = objectMapper;
    }

    public GitHubFactCheckResult check(
            String category,
            String title,
            String content,
            List<String> tags) {
        String joinedText = String.join(" ", normalize(category), normalize(title), normalize(content), formatTags(tags));
        Optional<RepositoryReference> repositoryReference = extractRepositoryReference(joinedText);

        if (repositoryReference.isEmpty()) {
            return emptyResult(
                    "NOT_SUPPORTED",
                    "이 게시글에서 GitHub 저장소 참조를 찾지 못했습니다.");
        }

        RepositoryReference reference = repositoryReference.get();
        McpServerService.McpToolCallResult toolCallResult =
                mcpServerService.callGitHubRepositoryTool(reference.owner(), reference.repo());
        GitHubApiClient.GitHubRepositoryReport repositoryReport = objectMapper.convertValue(
                toolCallResult.structuredContent(),
                GitHubApiClient.GitHubRepositoryReport.class);
        OpenAiTextClient.TextGenerationResult textGenerationResult =
                openAiTextClient.generateStructuredJson(
                        FACT_CHECK_INSTRUCTIONS,
                        buildPromptInput(category, title, content, tags, repositoryReport),
                        GITHUB_FACT_CHECK_SCHEMA);
        StructuredGitHubJudgement judgement = parseStructuredJudgement(textGenerationResult.text());

        return new GitHubFactCheckResult(
                "CHECKED",
                "게시글의 GitHub 저장소 관련 표현과 MCP 외부 정보를 비교했습니다.",
                GITHUB_TOOL_NAME,
                repositoryReport.fullName(),
                repositoryReport.htmlUrl(),
                repositoryReport.source(),
                repositoryReport.updatedAt(),
                repositoryReport.toFactText(),
                judgement.claim(),
                judgement.verdict(),
                judgement.comparison(),
                judgement.suggestion(),
                judgement.summary());
    }

    private GitHubFactCheckResult emptyResult(String status, String message) {
        return new GitHubFactCheckResult(
                status,
                message,
                GITHUB_TOOL_NAME,
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

    private static String buildPromptInput(
            String category,
            String title,
            String content,
            List<String> tags,
            GitHubApiClient.GitHubRepositoryReport repositoryReport) {
        return """
                Post:
                - category: %s
                - title: %s
                - tags: %s
                - content:
                %s

                Fetched GitHub repository metadata:
                %s

                Compare repository-related wording in the post with the fetched GitHub facts.
                Focus on concrete claims such as stars, forks, open issues, language, license,
                archived status, default branch, and recent activity.
                Return a JSON object that follows the supplied schema.
                """.formatted(
                normalize(category),
                normalize(title),
                formatTags(tags),
                normalize(content),
                repositoryReport.toFactText());
    }

    static Optional<RepositoryReference> extractRepositoryReference(String text) {
        String normalizedText = normalize(text);
        Matcher urlMatcher = GITHUB_URL_PATTERN.matcher(normalizedText);
        if (urlMatcher.find()) {
            return Optional.of(new RepositoryReference(
                    trimRepositoryPart(urlMatcher.group(1)),
                    trimRepositoryPart(urlMatcher.group(2))));
        }

        if (!hasGitHubIntent(normalizedText)) {
            return Optional.empty();
        }

        Matcher ownerRepoMatcher = OWNER_REPO_PATTERN.matcher(normalizedText);
        while (ownerRepoMatcher.find()) {
            String owner = trimRepositoryPart(ownerRepoMatcher.group(1));
            String repo = trimRepositoryPart(ownerRepoMatcher.group(2));
            if (isLikelyRepositoryPart(owner) && isLikelyRepositoryPart(repo)) {
                return Optional.of(new RepositoryReference(owner, repo));
            }
        }

        return Optional.empty();
    }

    private StructuredGitHubJudgement parseStructuredJudgement(String generatedText) {
        String jsonText = stripCodeFence(generatedText);

        try {
            StructuredGitHubJudgement judgement =
                    objectMapper.readValue(jsonText, StructuredGitHubJudgement.class);

            return new StructuredGitHubJudgement(
                    normalize(judgement.claim()),
                    normalizeVerdict(judgement.verdict()),
                    normalize(judgement.comparison()),
                    normalize(judgement.suggestion()),
                    normalize(judgement.summary()));
        } catch (JsonProcessingException exception) {
            String fallbackText = normalize(generatedText);

            return new StructuredGitHubJudgement(
                    "",
                    "uncertain",
                    fallbackText,
                    "",
                    fallbackText);
        }
    }

    private static boolean hasGitHubIntent(String text) {
        String lowerText = normalize(text).toLowerCase(Locale.ROOT);
        return lowerText.contains("github")
                || lowerText.contains("깃허브")
                || lowerText.contains("깃헙");
    }

    private static boolean isLikelyRepositoryPart(String value) {
        return value != null
                && !value.isBlank()
                && !value.contains("..")
                && value.length() <= 100
                && Character.isLetterOrDigit(value.charAt(0));
    }

    private static String trimRepositoryPart(String value) {
        if (value == null) {
            return "";
        }

        String trimmedValue = value.trim();
        return REPOSITORY_NAME_TRIM_PATTERN.matcher(trimmedValue).replaceAll("");
    }

    private static String stripCodeFence(String text) {
        String normalizedText = normalize(text);
        if (normalizedText.startsWith("```json")) {
            normalizedText = normalizedText.substring("```json".length()).trim();
        } else if (normalizedText.startsWith("```")) {
            normalizedText = normalizedText.substring("```".length()).trim();
        }

        if (normalizedText.endsWith("```")) {
            normalizedText = normalizedText.substring(0, normalizedText.length() - "```".length()).trim();
        }

        return normalizedText;
    }

    private static String normalizeVerdict(String verdict) {
        String normalizedVerdict = normalize(verdict).toLowerCase(Locale.ROOT)
                .replace(" ", "_");

        if (List.of("supported", "contradicted", "uncertain", "too_vague").contains(normalizedVerdict)) {
            return normalizedVerdict;
        }

        return "uncertain";
    }

    private static String formatTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "";
        }

        return String.join(", ", tags);
    }

    private static String normalize(String text) {
        if (text == null) {
            return "";
        }

        return text.trim();
    }

    public record RepositoryReference(String owner, String repo) {
    }

    private record StructuredGitHubJudgement(
            String claim,
            String verdict,
            String comparison,
            String suggestion,
            String summary) {
    }

    public record GitHubFactCheckResult(
            String status,
            String message,
            String toolName,
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
    }
}
