package com.jungle_choi.namanmu.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jungle_choi.namanmu.config.OpenAiProperties;
import com.jungle_choi.namanmu.domain.embedding.PostEmbedding;
import com.jungle_choi.namanmu.domain.embedding.PostEmbeddingRepository;
import com.jungle_choi.namanmu.domain.post.Post;
import com.jungle_choi.namanmu.domain.post.PostStatus;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SimilarPostSearchService {

    private static final int MAX_LIMIT = 10;
    private static final double MIN_RELEVANCE_SCORE = 0.38;
    private static final TypeReference<List<Double>> EMBEDDING_VECTOR_TYPE = new TypeReference<>() {
    };

    private final PostEmbeddingRepository postEmbeddingRepository;
    private final OpenAiProperties openAiProperties;
    private final ObjectMapper objectMapper;

    public SimilarPostSearchService(
            PostEmbeddingRepository postEmbeddingRepository,
            OpenAiProperties openAiProperties,
            ObjectMapper objectMapper) {
        this.postEmbeddingRepository = postEmbeddingRepository;
        this.openAiProperties = openAiProperties;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<SimilarPostResult> searchSimilarPosts(
            List<Double> queryEmbedding,
            Long excludedPostId,
            int limit) {
        return searchSimilarPosts(
                queryEmbedding,
                excludedPostId,
                limit,
                "",
                "",
                List.of());
    }

    @Transactional(readOnly = true)
    public List<SimilarPostResult> searchSimilarPosts(
            List<Double> queryEmbedding,
            Long excludedPostId,
            int limit,
            String title,
            String content,
            List<String> tags) {
        if (queryEmbedding == null || queryEmbedding.isEmpty()) {
            return List.of();
        }

        int normalizedLimit = normalizeLimit(limit);
        List<String> queryTerms = buildQueryTerms(title, content, tags);

        return postEmbeddingRepository.findAllByEmbeddingModel(openAiProperties.embeddingModel())
                .stream()
                .map((postEmbedding) -> toSimilarPostResult(
                        postEmbedding,
                        queryEmbedding,
                        queryTerms,
                        excludedPostId))
                .flatMap(Optional::stream)
                .sorted(Comparator.comparingDouble(SimilarPostResult::score).reversed())
                .limit(normalizedLimit)
                .toList();
    }

    private Optional<SimilarPostResult> toSimilarPostResult(
            PostEmbedding postEmbedding,
            List<Double> queryEmbedding,
            List<String> queryTerms,
            Long excludedPostId) {
        Post post = postEmbedding.getPost();

        if (post.getStatus() != PostStatus.PUBLISHED) {
            return Optional.empty();
        }

        if (Objects.equals(post.getId(), excludedPostId)) {
            return Optional.empty();
        }

        List<Double> storedEmbedding = parseEmbedding(postEmbedding);
        if (storedEmbedding.size() != queryEmbedding.size()) {
            return Optional.empty();
        }

        double score = cosineSimilarity(queryEmbedding, storedEmbedding);
        if (!queryTerms.isEmpty()) {
            score = Math.min((score * 0.35) + lexicalRelevanceScore(post, queryTerms), 1.0);
            if (score < MIN_RELEVANCE_SCORE) {
                return Optional.empty();
            }
        }

        return Optional.of(new SimilarPostResult(
                post.getId(),
                post.getTitle(),
                post.getCategory(),
                post.getContent(),
                score));
    }

    private List<Double> parseEmbedding(PostEmbedding postEmbedding) {
        try {
            return objectMapper.readValue(
                    postEmbedding.getEmbeddingJson(),
                    EMBEDDING_VECTOR_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored embedding vector could not be parsed.", exception);
        }
    }

    static double cosineSimilarity(List<Double> left, List<Double> right) {
        if (left == null || right == null || left.size() != right.size() || left.isEmpty()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double leftMagnitude = 0.0;
        double rightMagnitude = 0.0;

        for (int index = 0; index < left.size(); index++) {
            double leftValue = left.get(index);
            double rightValue = right.get(index);

            dotProduct += leftValue * rightValue;
            leftMagnitude += leftValue * leftValue;
            rightMagnitude += rightValue * rightValue;
        }

        if (leftMagnitude == 0.0 || rightMagnitude == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(leftMagnitude) * Math.sqrt(rightMagnitude));
    }

    private static double lexicalRelevanceScore(Post post, List<String> queryTerms) {
        String title = normalizeSearchText(post.getTitle());
        String category = normalizeSearchText(post.getCategory());
        String content = normalizeSearchText(post.getContent());
        double score = 0.0;
        int matchedTerms = 0;

        for (String term : queryTerms) {
            boolean matched = false;
            if (title.contains(term)) {
                score += 0.35;
                matched = true;
            }
            if (content.contains(term)) {
                score += 0.08;
                matched = true;
            }
            if (category.contains(term)) {
                score += 0.04;
                matched = true;
            }
            if (matched) {
                matchedTerms++;
            }
        }

        if (matchedTerms > 0) {
            score += Math.min(matchedTerms * 0.03, 0.12);
        }

        return Math.min(score, 0.65);
    }

    private static List<String> buildQueryTerms(String title, String content, List<String> tags) {
        String joinedTags = tags == null ? "" : String.join(" ", tags);
        String queryText = normalizeSearchText("%s %s %s".formatted(title, content, joinedTags));
        Set<String> terms = new LinkedHashSet<>();

        for (String token : queryText.split("[^\\p{L}\\p{N}]+")) {
            if (isMeaningfulTerm(token)) {
                terms.add(token);
            }
        }

        return terms.stream()
                .limit(12)
                .toList();
    }

    private static boolean isMeaningfulTerm(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        if (token.length() < 2) {
            return false;
        }

        return !Set.of(
                "this",
                "that",
                "with",
                "from",
                "about",
                "daily",
                "하고",
                "싶다",
                "정리",
                "사용",
                "내용",
                "관련",
                "게시글",
                "작성",
                "오늘",
                "그냥",
                "기분",
                "피곤",
                "피곤해서",
                "일상",
                "생각",
                "느낌",
                "이번",
                "정도",
                "부분",
                "때문",
                "통해")
                .contains(token);
    }

    private static String normalizeSearchText(String text) {
        if (text == null) {
            return "";
        }

        return text.toLowerCase(Locale.ROOT)
                .replace("깃허브", " github ")
                .replace("깃헙", " github ")
                .replace("깃랩", " gitlab ")
                .replace("깃 액션", " github actions ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static int normalizeLimit(int limit) {
        return Math.min(Math.max(limit, 1), MAX_LIMIT);
    }

    public record SimilarPostResult(
            Long postId,
            String title,
            String category,
            String content,
            double score) {
    }
}
