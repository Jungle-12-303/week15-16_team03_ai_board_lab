package com.jungle_choi.namanmu.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jungle_choi.namanmu.config.OpenAiProperties;
import com.jungle_choi.namanmu.domain.embedding.PostEmbedding;
import com.jungle_choi.namanmu.domain.embedding.PostEmbeddingRepository;
import com.jungle_choi.namanmu.domain.post.Post;
import com.jungle_choi.namanmu.domain.post.PostStatus;
import com.jungle_choi.namanmu.domain.post.PostTagRepository;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SimilarPostSearchService {

    private static final int MAX_LIMIT = 10;
    private static final double MIN_RELEVANCE_SCORE = 0.38;
    private static final double BM25_K1 = 1.2;
    private static final double BM25_B = 0.75;
    private static final double VECTOR_WEIGHT_WITH_QUERY_TERMS = 0.35;
    private static final double BM25_WEIGHT_WITH_QUERY_TERMS = 0.65;
    private static final TypeReference<List<Double>> EMBEDDING_VECTOR_TYPE = new TypeReference<>() {
    };

    private final PostEmbeddingRepository postEmbeddingRepository;
    private final PostTagRepository postTagRepository;
    private final OpenAiProperties openAiProperties;
    private final ObjectMapper objectMapper;

    public SimilarPostSearchService(
            PostEmbeddingRepository postEmbeddingRepository,
            PostTagRepository postTagRepository,
            OpenAiProperties openAiProperties,
            ObjectMapper objectMapper) {
        this.postEmbeddingRepository = postEmbeddingRepository;
        this.postTagRepository = postTagRepository;
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
                "",
                List.of());
    }

    @Transactional(readOnly = true)
    public List<SimilarPostResult> searchSimilarPosts(
            List<Double> queryEmbedding,
            Long excludedPostId,
            int limit,
            String category,
            String title,
            String content,
            List<String> tags) {
        if (queryEmbedding == null || queryEmbedding.isEmpty()) {
            return List.of();
        }

        int normalizedLimit = normalizeLimit(limit);
        List<String> queryTerms = buildQueryTerms(title, content, tags);
        SearchMetadata metadata = SearchMetadata.from(category, tags);
        List<PostEmbedding> candidates = postEmbeddingRepository.findAllByEmbeddingModel(openAiProperties.embeddingModel())
                .stream()
                .filter((postEmbedding) -> isSearchCandidate(postEmbedding, metadata, excludedPostId))
                .toList();
        Bm25CorpusStats bm25CorpusStats = Bm25CorpusStats.from(
                candidates.stream()
                        .map(PostEmbedding::getPost)
                        .toList(),
                queryTerms);

        return candidates.stream()
                .map((postEmbedding) -> toSimilarPostResult(
                        postEmbedding,
                        queryEmbedding,
                        queryTerms,
                        bm25CorpusStats))
                .flatMap(Optional::stream)
                .sorted(Comparator.comparingDouble(SimilarPostResult::score).reversed())
                .limit(normalizedLimit)
                .toList();
    }

    private boolean isSearchCandidate(
            PostEmbedding postEmbedding,
            SearchMetadata metadata,
            Long excludedPostId) {
        Post post = postEmbedding.getPost();

        if (post.getStatus() != PostStatus.PUBLISHED) {
            return false;
        }

        if (Objects.equals(post.getId(), excludedPostId)) {
            return false;
        }

        return matchesMetadata(post, metadata);
    }

    private Optional<SimilarPostResult> toSimilarPostResult(
            PostEmbedding postEmbedding,
            List<Double> queryEmbedding,
            List<String> queryTerms,
            Bm25CorpusStats bm25CorpusStats) {
        Post post = postEmbedding.getPost();

        List<Double> storedEmbedding = parseEmbedding(postEmbedding);
        if (storedEmbedding.size() != queryEmbedding.size()) {
            return Optional.empty();
        }

        double score = cosineSimilarity(queryEmbedding, storedEmbedding);
        if (!queryTerms.isEmpty()) {
            double bm25Score = bm25Score(post, queryTerms, bm25CorpusStats);
            score = Math.min(
                    (score * VECTOR_WEIGHT_WITH_QUERY_TERMS)
                            + (bm25Score * BM25_WEIGHT_WITH_QUERY_TERMS),
                    1.0);
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

    private static double bm25Score(
            Post post,
            List<String> queryTerms,
            Bm25CorpusStats corpusStats) {
        if (queryTerms.isEmpty() || corpusStats.documentCount() == 0) {
            return 0.0;
        }

        List<String> documentTerms = buildDocumentTerms(post);
        int documentLength = documentTerms.size();
        if (documentLength == 0) {
            return 0.0;
        }

        Map<String, Integer> termFrequencies = new HashMap<>();
        for (String term : documentTerms) {
            termFrequencies.merge(term, 1, Integer::sum);
        }

        double rawScore = 0.0;
        for (String queryTerm : queryTerms) {
            int termFrequency = termFrequencies.getOrDefault(queryTerm, 0);
            int documentFrequency = corpusStats.documentFrequency(queryTerm);

            if (termFrequency == 0 || documentFrequency == 0) {
                continue;
            }

            double idf = Math.log(1.0
                    + ((corpusStats.documentCount() - documentFrequency + 0.5)
                            / (documentFrequency + 0.5)));
            double lengthNormalization = 1.0 - BM25_B
                    + (BM25_B * documentLength / corpusStats.averageDocumentLength());
            double saturatedTermFrequency =
                    (termFrequency * (BM25_K1 + 1.0))
                            / (termFrequency + (BM25_K1 * lengthNormalization));

            rawScore += idf * saturatedTermFrequency;
        }

        return 1.0 - Math.exp(-rawScore);
    }

    private boolean matchesMetadata(Post post, SearchMetadata metadata) {
        if (!metadata.hasFilters()) {
            return true;
        }

        if (!metadata.category().isBlank()
                && !normalizeSearchText(post.getCategory()).equals(metadata.category())) {
            return false;
        }

        if (metadata.tags().isEmpty()) {
            return true;
        }

        Set<String> postTags = postTagRepository.findAllByPostIdOrderByTagNameAsc(post.getId())
                .stream()
                .map((postTag) -> normalizeSearchText(postTag.getTag().getName()))
                .collect(java.util.stream.Collectors.toSet());

        return metadata.tags().stream().anyMatch(postTags::contains);
    }

    private static List<String> buildQueryTerms(String title, String content, List<String> tags) {
        String joinedTags = tags == null ? "" : String.join(" ", tags);
        Set<String> terms = new LinkedHashSet<>(tokenizeSearchText("%s %s %s".formatted(
                title,
                content,
                joinedTags)));

        return terms.stream()
                .limit(12)
                .toList();
    }

    private static List<String> buildDocumentTerms(Post post) {
        return tokenizeSearchText("%s %s %s %s".formatted(
                post.getTitle(),
                post.getTitle(),
                post.getCategory(),
                post.getContent()));
    }

    private static List<String> tokenizeSearchText(String text) {
        String normalizedText = normalizeSearchText(text);

        return java.util.Arrays.stream(normalizedText.split("[^\\p{L}\\p{N}]+"))
                .filter(SimilarPostSearchService::isMeaningfulTerm)
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

    private record SearchMetadata(String category, Set<String> tags) {

        static SearchMetadata from(String category, List<String> tags) {
            String normalizedCategory = normalizeSearchText(category);
            if ("all".equals(normalizedCategory)) {
                normalizedCategory = "";
            }

            Set<String> normalizedTags = tags == null
                    ? Set.of()
                    : tags.stream()
                            .map(SimilarPostSearchService::normalizeSearchText)
                            .filter((tag) -> !tag.isBlank())
                            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

            return new SearchMetadata(normalizedCategory, normalizedTags);
        }

        boolean hasFilters() {
            return !category.isBlank() || !tags.isEmpty();
        }
    }

    private record Bm25CorpusStats(
            int documentCount,
            double averageDocumentLength,
            Map<String, Integer> documentFrequencies) {

        static Bm25CorpusStats from(List<Post> posts, List<String> queryTerms) {
            if (posts.isEmpty() || queryTerms.isEmpty()) {
                return new Bm25CorpusStats(posts.size(), 1.0, Map.of());
            }

            Map<String, Integer> documentFrequencies = new HashMap<>();
            int totalDocumentLength = 0;

            for (Post post : posts) {
                List<String> documentTerms = buildDocumentTerms(post);
                totalDocumentLength += documentTerms.size();

                Set<String> uniqueDocumentTerms = new HashSet<>(documentTerms);
                for (String queryTerm : queryTerms) {
                    if (uniqueDocumentTerms.contains(queryTerm)) {
                        documentFrequencies.merge(queryTerm, 1, Integer::sum);
                    }
                }
            }

            double averageDocumentLength = Math.max(
                    totalDocumentLength / (double) posts.size(),
                    1.0);

            return new Bm25CorpusStats(
                    posts.size(),
                    averageDocumentLength,
                    documentFrequencies);
        }

        int documentFrequency(String term) {
            return documentFrequencies.getOrDefault(term, 0);
        }
    }

    public record SimilarPostResult(
            Long postId,
            String title,
            String category,
            String content,
            double score) {
    }
}
