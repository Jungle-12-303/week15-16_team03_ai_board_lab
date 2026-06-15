package com.jungle_choi.namanmu.service;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class RagDraftService {

    private static final int DEFAULT_SOURCE_LIMIT = 5;
    private static final int MAX_PROMPT_SOURCE_COUNT = 3;
    private static final double MIN_PROMPT_SOURCE_SCORE = 0.78;
    private static final double MIN_PROMPT_KEYWORD_ALIGNMENT_SCORE = 0.25;
    private static final double MIN_PROMPT_BM25_SCORE = 0.08;
    private static final String DRAFT_INSTRUCTIONS = """
            You are a writing assistant for Project Alpha, a Korean board for development, learning,
            project notes, reviews, briefings, and daily logs.

            Write only the post body in Korean.
            The user's draft is the primary source of intent. Preserve the user's topic, scope,
            point of view, and level of specificity.
            Retrieved posts are optional reference material. Use them only when they directly help
            the user's draft. Ignore unrelated sources even if they were retrieved.
            Do not introduce unrelated companies, products, incidents, numbers, or technologies.
            If the retrieved posts do not support a claim, do not invent specific facts.
            Do not transfer facts across different products, regions, repositories, APIs, or technologies.
            If a source only matches the broad category, use it for structure or tone, not factual claims.
            Keep the draft coherent, practical, and ready to edit.
            """;

    private final PostEmbeddingTextBuilder postEmbeddingTextBuilder;
    private final OpenAiEmbeddingClient openAiEmbeddingClient;
    private final SimilarPostSearchService similarPostSearchService;
    private final OpenAiTextClient openAiTextClient;

    public RagDraftService(
            PostEmbeddingTextBuilder postEmbeddingTextBuilder,
            OpenAiEmbeddingClient openAiEmbeddingClient,
            SimilarPostSearchService similarPostSearchService,
            OpenAiTextClient openAiTextClient) {
        this.postEmbeddingTextBuilder = postEmbeddingTextBuilder;
        this.openAiEmbeddingClient = openAiEmbeddingClient;
        this.similarPostSearchService = similarPostSearchService;
        this.openAiTextClient = openAiTextClient;
    }

    public RagDraftResult createDraft(
            String category,
            String title,
            String content,
            List<String> tags,
            Long excludedPostId,
            int limit) {
        String queryText = postEmbeddingTextBuilder.buildQuery(category, title, content, tags);
        OpenAiEmbeddingClient.EmbeddingResult embeddingResult =
                openAiEmbeddingClient.createEmbedding(queryText);

        List<SimilarPostSearchService.SimilarPostResult> similarPosts =
                similarPostSearchService.searchSimilarPosts(
                        embeddingResult.embedding(),
                        excludedPostId,
                        normalizeLimit(limit),
                        category,
                        title,
                        content,
                        tags);

        if (similarPosts.isEmpty()) {
            return new RagDraftResult(
                    "",
                    List.of(),
                    "초안 생성에 사용할 유사 게시글이 없습니다. 임베딩 작업이 완료된 뒤 다시 시도해 주세요.");
        }

        List<SimilarPostSearchService.SimilarPostResult> promptSources =
                selectPromptSources(title, content, tags, similarPosts);

        if (promptSources.isEmpty()) {
            return new RagDraftResult(
                    "",
                    List.of(),
                    "초안 생성에 사용할 만큼 직접적인 유사 게시글이 없습니다. Related posts에서 후보를 먼저 확인해 주세요.");
        }

        String promptInput = buildPromptInput(category, title, content, tags, promptSources);
        OpenAiTextClient.TextGenerationResult textGenerationResult =
                openAiTextClient.generateText(DRAFT_INSTRUCTIONS, promptInput);

        return new RagDraftResult(
                textGenerationResult.text(),
                promptSources.stream()
                        .map(RagDraftSource::from)
                        .toList(),
                "직접 관련성이 높은 유사 게시글 %d개를 근거로 초안을 생성했습니다."
                        .formatted(promptSources.size()));
    }

    private static List<SimilarPostSearchService.SimilarPostResult> selectPromptSources(
            String title,
            String content,
            List<String> tags,
            List<SimilarPostSearchService.SimilarPostResult> similarPosts) {
        Set<String> anchorTerms = buildPromptAnchorTerms(title, content, tags);
        Set<String> requiredTerms = buildPromptRequiredTerms(title, content, tags);
        Set<String> requiredLocationTerms = buildPromptLocationTerms(title, content, tags);

        return similarPosts.stream()
                .filter((source) -> isStrongPromptSource(
                        source,
                        anchorTerms,
                        requiredTerms,
                        requiredLocationTerms))
                .limit(MAX_PROMPT_SOURCE_COUNT)
                .toList();
    }

    private static boolean isStrongPromptSource(
            SimilarPostSearchService.SimilarPostResult source,
            Set<String> anchorTerms,
            Set<String> requiredTerms,
            Set<String> requiredLocationTerms) {
        if (source.score() < MIN_PROMPT_SOURCE_SCORE) {
            return false;
        }

        SimilarPostSearchService.SearchScoreBreakdown breakdown = source.scoreBreakdown();
        if (breakdown == null) {
            return true;
        }

        List<String> matchedTerms = breakdown.matchedTerms();
        if (!requiredLocationTerms.isEmpty() && matchedTerms.stream().noneMatch(requiredLocationTerms::contains)) {
            return false;
        }

        if (!requiredTerms.isEmpty() && matchedTerms.stream().noneMatch(requiredTerms::contains)) {
            return false;
        }

        if (!anchorTerms.isEmpty() && matchedTerms.stream().noneMatch(anchorTerms::contains)) {
            return false;
        }

        return breakdown.keywordAlignmentScore() >= MIN_PROMPT_KEYWORD_ALIGNMENT_SCORE
                || breakdown.bm25Score() >= MIN_PROMPT_BM25_SCORE
                || matchedTerms.size() >= 2;
    }

    private static Set<String> buildPromptAnchorTerms(
            String title,
            String content,
            List<String> tags) {
        String text = "%s %s %s".formatted(
                normalize(title),
                normalize(content),
                tags == null ? "" : String.join(" ", tags));

        return java.util.Arrays.stream(normalizeAnchorText(text).split("[^\\p{L}\\p{N}]+"))
                .filter(RagDraftService::isPromptAnchorTerm)
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    private static Set<String> buildPromptRequiredTerms(
            String title,
            String content,
            List<String> tags) {
        String text = "%s %s %s".formatted(
                normalize(title),
                normalize(content),
                tags == null ? "" : String.join(" ", tags));

        return java.util.Arrays.stream(normalizeAnchorText(text).split("[^\\p{L}\\p{N}]+"))
                .filter(RagDraftService::isPromptRequiredTerm)
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    private static Set<String> buildPromptLocationTerms(
            String title,
            String content,
            List<String> tags) {
        String text = "%s %s %s".formatted(
                normalize(title),
                normalize(content),
                tags == null ? "" : String.join(" ", tags));

        return java.util.Arrays.stream(normalizeAnchorText(text).split("[^\\p{L}\\p{N}]+"))
                .filter(RagDraftService::isPromptLocationTerm)
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    private static String normalizeAnchorText(String text) {
        return normalize(text).toLowerCase(Locale.ROOT)
                .replace("깃허브 액션", " github actions ")
                .replace("깃헙 액션", " github actions ")
                .replace("깃허브", " github ")
                .replace("깃헙", " github ")
                .replace("깃 액션", " github actions ")
                .replace("github action", " github actions ")
                .replace("워크플로우", " workflow ")
                .replace("워크플로", " workflow ")
                .replace("시크릿", " secrets ")
                .replace("비밀값", " secrets ")
                .replace("배포", " deploy ")
                .replace("시피유", " cpu ")
                .replace("씨피유", " cpu ")
                .replace("그래픽 카드", " gpu ")
                .replace("그래픽카드", " gpu ")
                .replace("리액트", " react ")
                .replace("훅", " hook ")
                .replace("스프링부트", " spring boot ")
                .replace("스프링 부트", " spring boot ")
                .replace("인증", " security ")
                .replace("날씨", " weather ")
                .replace("비가", " rain ")
                .replace("비는", " rain ")
                .replace("비를", " rain ")
                .replace("언더볼팅", " undervolt ")
                .replace("팬 소음", " fan noise ");
    }

    private static boolean isPromptAnchorTerm(String term) {
        return Set.of(
                "cpu",
                "gpu",
                "react",
                "hook",
                "usestate",
                "useeffect",
                "spring",
                "boot",
                "jwt",
                "security",
                "github",
                "actions",
                "workflow",
                "deploy",
                "ci",
                "secrets",
                "weather",
                "rain",
                "forecast",
                "fan",
                "noise",
                "undervolt")
                .contains(term)
                || term.matches("\\d+[a-z0-9]*");
    }

    private static boolean isPromptRequiredTerm(String term) {
        return Set.of(
                "jwt",
                "security",
                "cors",
                "authorization",
                "403",
                "github",
                "actions",
                "workflow",
                "deploy",
                "ci",
                "secrets",
                "ssh",
                "ec2",
                "react",
                "hook",
                "usestate",
                "useeffect",
                "cpu",
                "gpu",
                "undervolt",
                "weather",
                "rain",
                "forecast")
                .contains(term);
    }

    private static boolean isPromptLocationTerm(String term) {
        return Set.of(
                "서울",
                "부산",
                "대구",
                "인천",
                "광주",
                "대전",
                "울산",
                "세종",
                "제주",
                "수원",
                "용인",
                "성남",
                "고양",
                "창원",
                "청주",
                "천안",
                "전주",
                "포항",
                "김해",
                "진주",
                "춘천",
                "강릉")
                .contains(term);
    }

    private static String buildPromptInput(
            String category,
            String title,
            String content,
            List<String> tags,
            List<SimilarPostSearchService.SimilarPostResult> similarPosts) {
        return """
                User draft:
                Category: %s
                Title: %s
                Tags: %s
                Current content:
                %s

                Retrieved posts:
                %s

                Task:
                Rewrite the current content into a stronger first draft for this board post.
                Keep the user's original topic first. Use retrieved posts only as light reference.
                Do not change the subject just because a retrieved post discusses another topic.
                The output must be only the body text, without a title.
                """.formatted(
                normalize(category),
                normalize(title),
                formatTags(tags),
                normalize(content),
                formatSources(similarPosts));
    }

    private static String formatSources(
            List<SimilarPostSearchService.SimilarPostResult> similarPosts) {
        StringBuilder builder = new StringBuilder();

        for (int index = 0; index < similarPosts.size(); index++) {
            SimilarPostSearchService.SimilarPostResult post = similarPosts.get(index);
            builder.append(index + 1)
                    .append(". ")
                    .append("postId=")
                    .append(post.postId())
                    .append(", category=")
                    .append(normalize(post.category()))
                    .append(", score=")
                    .append(String.format(Locale.US, "%.3f", post.score()))
                    .append("\nMatched terms: ")
                    .append(formatMatchedTerms(post))
                    .append("\nEvidence caution: Use this source for factual claims only when the matched terms")
                    .append(" cover the user's exact subject.")
                    .append("\nTitle: ")
                    .append(normalize(post.title()))
                    .append("\nContent excerpt:\n")
                    .append(excerpt(post.content(), 900))
                    .append("\n\n");
        }

        return builder.toString().trim();
    }

    private static String formatMatchedTerms(SimilarPostSearchService.SimilarPostResult post) {
        SimilarPostSearchService.SearchScoreBreakdown breakdown = post.scoreBreakdown();
        if (breakdown == null || breakdown.matchedTerms().isEmpty()) {
            return "None";
        }

        return String.join(", ", breakdown.matchedTerms());
    }

    private static String formatTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "None";
        }

        String joinedTags = String.join(", ", tags.stream()
                .map(RagDraftService::normalize)
                .filter((tag) -> !tag.isBlank())
                .toList());

        if (joinedTags.isBlank()) {
            return "None";
        }

        return joinedTags;
    }

    private static String excerpt(String text, int maxLength) {
        String normalizedText = normalize(text);

        if (normalizedText.length() <= maxLength) {
            return normalizedText;
        }

        return normalizedText.substring(0, maxLength) + "...";
    }

    private static String normalize(String text) {
        if (text == null) {
            return "";
        }

        return text.trim();
    }

    private static int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_SOURCE_LIMIT;
        }

        return limit;
    }

    public record RagDraftResult(
            String draft,
            List<RagDraftSource> sources,
            String message) {
    }

    public record RagDraftSource(
            Long postId,
            String title,
            String category,
            String content,
            double score,
            SimilarPostSearchService.SearchScoreBreakdown scoreBreakdown) {

        static RagDraftSource from(SimilarPostSearchService.SimilarPostResult result) {
            return new RagDraftSource(
                    result.postId(),
                    result.title(),
                    result.category(),
                    result.content(),
                    result.score(),
                    result.scoreBreakdown());
        }
    }
}
