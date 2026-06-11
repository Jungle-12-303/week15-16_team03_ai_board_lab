package com.jungle_choi.namanmu.service;

import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class RagDraftService {

    private static final int DEFAULT_SOURCE_LIMIT = 5;
    private static final String DRAFT_INSTRUCTIONS = """
            You are a writing assistant for Project Alpha, a Korean board for development, learning,
            project notes, reviews, briefings, and daily logs.

            Write only the post body in Korean.
            Use the retrieved posts as reference material, but do not copy their sentences.
            Keep the user's original intent and category.
            If the sources do not support a claim, do not invent specific facts.
            Make the draft coherent, practical, and ready to edit.
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
        String queryText = postEmbeddingTextBuilder.build(category, title, content, tags);
        OpenAiEmbeddingClient.EmbeddingResult embeddingResult =
                openAiEmbeddingClient.createEmbedding(queryText);

        List<SimilarPostSearchService.SimilarPostResult> similarPosts =
                similarPostSearchService.searchSimilarPosts(
                        embeddingResult.embedding(),
                        excludedPostId,
                        normalizeLimit(limit));

        if (similarPosts.isEmpty()) {
            return new RagDraftResult(
                    "",
                    List.of(),
                    "초안 생성에 사용할 유사 게시글이 없습니다. 임베딩 작업이 완료된 뒤 다시 시도해 주세요.");
        }

        String promptInput = buildPromptInput(category, title, content, tags, similarPosts);
        OpenAiTextClient.TextGenerationResult textGenerationResult =
                openAiTextClient.generateText(DRAFT_INSTRUCTIONS, promptInput);

        return new RagDraftResult(
                textGenerationResult.text(),
                similarPosts.stream()
                        .map(RagDraftSource::from)
                        .toList(),
                "유사 게시글을 근거로 초안을 생성했습니다.");
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
                    .append("\nTitle: ")
                    .append(normalize(post.title()))
                    .append("\nContent excerpt:\n")
                    .append(excerpt(post.content(), 900))
                    .append("\n\n");
        }

        return builder.toString().trim();
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
            double score) {

        static RagDraftSource from(SimilarPostSearchService.SimilarPostResult result) {
            return new RagDraftSource(
                    result.postId(),
                    result.title(),
                    result.category(),
                    result.score());
        }
    }
}
