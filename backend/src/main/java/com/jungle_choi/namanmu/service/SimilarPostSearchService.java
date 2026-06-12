package com.jungle_choi.namanmu.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jungle_choi.namanmu.config.OpenAiProperties;
import com.jungle_choi.namanmu.domain.embedding.PostEmbedding;
import com.jungle_choi.namanmu.domain.embedding.PostEmbeddingChunk;
import com.jungle_choi.namanmu.domain.embedding.PostEmbeddingChunkRepository;
import com.jungle_choi.namanmu.domain.embedding.PostEmbeddingRepository;
import com.jungle_choi.namanmu.domain.post.Post;
import com.jungle_choi.namanmu.domain.post.PostStatus;
import com.jungle_choi.namanmu.domain.post.PostTagRepository;
import java.util.ArrayList;
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
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
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
    private static final double CHUNK_EVIDENCE_WEIGHT = 0.05;
    private static final double RRF_RANK_CONSTANT = 60.0;
    private static final TypeReference<List<Double>> EMBEDDING_VECTOR_TYPE = new TypeReference<>() {
    };

    private final PostEmbeddingRepository postEmbeddingRepository;
    private final PostEmbeddingChunkRepository postEmbeddingChunkRepository;
    private final PostTagRepository postTagRepository;
    private final OpenAiProperties openAiProperties;
    private final ObjectMapper objectMapper;

    public SimilarPostSearchService(
            PostEmbeddingRepository postEmbeddingRepository,
            PostEmbeddingChunkRepository postEmbeddingChunkRepository,
            PostTagRepository postTagRepository,
            OpenAiProperties openAiProperties,
            ObjectMapper objectMapper) {
        this.postEmbeddingRepository = postEmbeddingRepository;
        this.postEmbeddingChunkRepository = postEmbeddingChunkRepository;
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
        List<PostEmbeddingChunk> chunkCandidates = findChunkCandidates(metadata, excludedPostId);
        List<PostEmbedding> candidates = findPostCandidates(metadata, excludedPostId);

        return searchSimilarPostsByCandidates(
                queryEmbedding,
                normalizedLimit,
                queryTerms,
                chunkCandidates,
                candidates);
    }

    private List<SimilarPostResult> searchSimilarPostsByCandidates(
            List<Double> queryEmbedding,
            int normalizedLimit,
            List<String> queryTerms,
            List<PostEmbeddingChunk> chunkCandidates,
            List<PostEmbedding> candidates) {
        Bm25CorpusStats postBm25CorpusStats = Bm25CorpusStats.from(
                candidates.stream()
                .map(PostEmbedding::getPost)
                .map(SimilarPostSearchService::buildDocumentTerms)
                        .toList(),
                queryTerms);

        List<ScoredCandidate> postScoredCandidates = new ArrayList<>();
        candidates.stream()
                .map((postEmbedding) -> toScoredCandidate(
                        postEmbedding,
                        queryEmbedding,
                        queryTerms,
                        postBm25CorpusStats))
                .flatMap(Optional::stream)
                .forEach(postScoredCandidates::add);

        List<SimilarPostResult> postResults =
                rankCandidates(postScoredCandidates, !queryTerms.isEmpty());
        Map<Long, Double> chunkScoresByPostId = scoreChunksByPostId(
                chunkCandidates,
                queryEmbedding,
                queryTerms);

        return postResults.stream()
                .map((result) -> applyChunkEvidenceBoost(result, chunkScoresByPostId))
                .sorted(Comparator.comparingDouble(SimilarPostResult::score).reversed()
                        .thenComparing(SimilarPostResult::postId))
                .limit(normalizedLimit)
                .toList();
    }

    private Map<Long, Double> scoreChunksByPostId(
            List<PostEmbeddingChunk> chunkCandidates,
            List<Double> queryEmbedding,
            List<String> queryTerms) {
        if (chunkCandidates.isEmpty()) {
            return Map.of();
        }

        Bm25CorpusStats chunkBm25CorpusStats = Bm25CorpusStats.from(
                chunkCandidates.stream()
                        .map(SimilarPostSearchService::buildChunkDocumentTerms)
                        .toList(),
                queryTerms);

        List<ScoredCandidate> chunkScoredCandidates = chunkCandidates.stream()
                .map((chunk) -> toScoredChunkCandidate(
                        chunk,
                        queryEmbedding,
                        queryTerms,
                        chunkBm25CorpusStats))
                .flatMap(Optional::stream)
                .toList();

        return aggregateBestCandidatePerPost(rankCandidates(chunkScoredCandidates, !queryTerms.isEmpty()))
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        SimilarPostResult::postId,
                        SimilarPostResult::score,
                        Math::max));
    }

    private static SimilarPostResult applyChunkEvidenceBoost(
            SimilarPostResult postResult,
            Map<Long, Double> chunkScoresByPostId) {
        double chunkScore = chunkScoresByPostId.getOrDefault(postResult.postId(), 0.0);
        double boostedScore = (postResult.score() * (1.0 - CHUNK_EVIDENCE_WEIGHT))
                + (chunkScore * CHUNK_EVIDENCE_WEIGHT);

        return new SimilarPostResult(
                postResult.postId(),
                postResult.title(),
                postResult.category(),
                postResult.content(),
                boostedScore);
    }

    private List<PostEmbeddingChunk> findChunkCandidates(SearchMetadata metadata, Long excludedPostId) {
        List<PostEmbeddingChunk> chunks =
                postEmbeddingChunkRepository.findAllByEmbeddingModel(openAiProperties.embeddingModel());

        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }

        return chunks.stream()
                .filter((chunk) -> isSearchCandidate(chunk, metadata, excludedPostId))
                .toList();
    }

    private List<PostEmbedding> findPostCandidates(
            SearchMetadata metadata,
            Long excludedPostId) {
        List<PostEmbedding> embeddings =
                postEmbeddingRepository.findAllByEmbeddingModel(openAiProperties.embeddingModel());

        if (embeddings == null || embeddings.isEmpty()) {
            return List.of();
        }

        return embeddings.stream()
                .filter((postEmbedding) -> isSearchCandidate(postEmbedding, metadata, excludedPostId))
                .toList();
    }

    private boolean isSearchCandidate(
            PostEmbedding postEmbedding,
            SearchMetadata metadata,
            Long excludedPostId) {
        return isSearchCandidate(postEmbedding.getPost(), metadata, excludedPostId);
    }

    private boolean isSearchCandidate(
            PostEmbeddingChunk postEmbeddingChunk,
            SearchMetadata metadata,
            Long excludedPostId) {
        return isSearchCandidate(postEmbeddingChunk.getPost(), metadata, excludedPostId);
    }

    private boolean isSearchCandidate(
            Post post,
            SearchMetadata metadata,
            Long excludedPostId) {
        if (post.getStatus() != PostStatus.PUBLISHED) {
            return false;
        }

        if (Objects.equals(post.getId(), excludedPostId)) {
            return false;
        }

        return matchesMetadata(post, metadata);
    }

    private Optional<ScoredCandidate> toScoredCandidate(
            PostEmbedding postEmbedding,
            List<Double> queryEmbedding,
            List<String> queryTerms,
            Bm25CorpusStats bm25CorpusStats) {
        Post post = postEmbedding.getPost();

        List<Double> storedEmbedding = parseEmbedding(postEmbedding);
        if (storedEmbedding.size() != queryEmbedding.size()) {
            return Optional.empty();
        }

        double vectorScore = cosineSimilarity(queryEmbedding, storedEmbedding);
        double bm25Score = 0.0;
        if (!queryTerms.isEmpty()) {
            bm25Score = bm25Score(post, queryTerms, bm25CorpusStats);
            double hybridRelevanceScore = Math.min(
                    (vectorScore * VECTOR_WEIGHT_WITH_QUERY_TERMS)
                            + (bm25Score * BM25_WEIGHT_WITH_QUERY_TERMS),
                    1.0);
            if (hybridRelevanceScore < MIN_RELEVANCE_SCORE) {
                return Optional.empty();
            }
        }

        return Optional.of(new ScoredCandidate("post:%d".formatted(post.getId()), post, vectorScore, bm25Score));
    }

    private Optional<ScoredCandidate> toScoredChunkCandidate(
            PostEmbeddingChunk postEmbeddingChunk,
            List<Double> queryEmbedding,
            List<String> queryTerms,
            Bm25CorpusStats bm25CorpusStats) {
        Post post = postEmbeddingChunk.getPost();

        List<Double> storedEmbedding = parseEmbedding(postEmbeddingChunk.getEmbeddingJson());
        if (storedEmbedding.size() != queryEmbedding.size()) {
            return Optional.empty();
        }

        double vectorScore = cosineSimilarity(queryEmbedding, storedEmbedding);
        double bm25Score = 0.0;
        if (!queryTerms.isEmpty()) {
            bm25Score = bm25Score(
                    buildChunkDocumentTerms(postEmbeddingChunk),
                    queryTerms,
                    bm25CorpusStats);
            double hybridRelevanceScore = Math.min(
                    (vectorScore * VECTOR_WEIGHT_WITH_QUERY_TERMS)
                            + (bm25Score * BM25_WEIGHT_WITH_QUERY_TERMS),
                    1.0);
            if (hybridRelevanceScore < MIN_RELEVANCE_SCORE) {
                return Optional.empty();
            }
        }

        return Optional.of(new ScoredCandidate(
                "chunk:%d".formatted(postEmbeddingChunk.getId()),
                post,
                vectorScore,
                bm25Score));
    }

    private List<Double> parseEmbedding(PostEmbedding postEmbedding) {
        return parseEmbedding(postEmbedding.getEmbeddingJson());
    }

    private List<Double> parseEmbedding(String embeddingJson) {
        try {
            return objectMapper.readValue(
                    embeddingJson,
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
        return bm25Score(buildDocumentTerms(post), queryTerms, corpusStats);
    }

    private static double bm25Score(
            List<String> documentTerms,
            List<String> queryTerms,
            Bm25CorpusStats corpusStats) {
        if (queryTerms.isEmpty() || corpusStats.documentCount() == 0) {
            return 0.0;
        }

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

    private static List<SimilarPostResult> aggregateBestCandidatePerPost(List<SimilarPostResult> chunkResults) {
        Map<Long, SimilarPostResult> bestResultsByPostId = new HashMap<>();

        for (SimilarPostResult chunkResult : chunkResults) {
            bestResultsByPostId.merge(
                    chunkResult.postId(),
                    chunkResult,
                    (current, next) -> current.score() >= next.score() ? current : next);
        }

        return List.copyOf(bestResultsByPostId.values());
    }

    private static List<SimilarPostResult> rankCandidates(
            List<ScoredCandidate> candidates,
            boolean useHybridFusion) {
        if (!useHybridFusion) {
            return candidates.stream()
                    .map((candidate) -> candidate.toResult(candidate.vectorScore()))
                    .toList();
        }

        Map<String, Integer> vectorRanks = rankBy(
                candidates,
                ScoredCandidate::vectorScore,
                (candidate) -> candidate.vectorScore() > 0.0);
        Map<String, Integer> bm25Ranks = rankBy(
                candidates,
                ScoredCandidate::bm25Score,
                (candidate) -> candidate.bm25Score() > 0.0);

        return candidates.stream()
                .map((candidate) -> candidate.toResult(rrfScore(candidate, vectorRanks, bm25Ranks)))
                .toList();
    }

    private static Map<String, Integer> rankBy(
            List<ScoredCandidate> candidates,
            ToDoubleFunction<ScoredCandidate> scoreExtractor,
            Predicate<ScoredCandidate> filter) {
        List<ScoredCandidate> rankedCandidates = candidates.stream()
                .filter(filter)
                .sorted((left, right) -> {
                    int scoreComparison = Double.compare(
                            scoreExtractor.applyAsDouble(right),
                            scoreExtractor.applyAsDouble(left));
                    if (scoreComparison != 0) {
                        return scoreComparison;
                    }

                    return Long.compare(left.post().getId(), right.post().getId());
                })
                .toList();
        Map<String, Integer> ranks = new HashMap<>();

        for (int index = 0; index < rankedCandidates.size(); index++) {
            ranks.put(rankedCandidates.get(index).rankId(), index + 1);
        }

        return ranks;
    }

    private static double rrfScore(
            ScoredCandidate candidate,
            Map<String, Integer> vectorRanks,
            Map<String, Integer> bm25Ranks) {
        String rankId = candidate.rankId();
        double rawScore = reciprocalRankScore(vectorRanks.get(rankId))
                + reciprocalRankScore(bm25Ranks.get(rankId));
        double maxPossibleScore = 2.0 / (RRF_RANK_CONSTANT + 1.0);

        return Math.min(rawScore / maxPossibleScore, 1.0);
    }

    private static double reciprocalRankScore(Integer rank) {
        if (rank == null) {
            return 0.0;
        }

        return 1.0 / (RRF_RANK_CONSTANT + rank);
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

    private static List<String> buildChunkDocumentTerms(PostEmbeddingChunk chunk) {
        Post post = chunk.getPost();

        return tokenizeSearchText("%s %s %s %s".formatted(
                post.getTitle(),
                post.getTitle(),
                post.getCategory(),
                chunk.getChunkText()));
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

        static Bm25CorpusStats from(List<List<String>> termDocuments, List<String> queryTerms) {
            if (termDocuments.isEmpty() || queryTerms.isEmpty()) {
                return new Bm25CorpusStats(termDocuments.size(), 1.0, Map.of());
            }

            Map<String, Integer> documentFrequencies = new HashMap<>();
            int totalDocumentLength = 0;

            for (List<String> documentTerms : termDocuments) {
                totalDocumentLength += documentTerms.size();

                Set<String> uniqueDocumentTerms = new HashSet<>(documentTerms);
                for (String queryTerm : queryTerms) {
                    if (uniqueDocumentTerms.contains(queryTerm)) {
                        documentFrequencies.merge(queryTerm, 1, Integer::sum);
                    }
                }
            }

            double averageDocumentLength = Math.max(
                    totalDocumentLength / (double) termDocuments.size(),
                    1.0);

            return new Bm25CorpusStats(
                    termDocuments.size(),
                    averageDocumentLength,
                    documentFrequencies);
        }

        int documentFrequency(String term) {
            return documentFrequencies.getOrDefault(term, 0);
        }
    }

    private record ScoredCandidate(
            String rankId,
            Post post,
            double vectorScore,
            double bm25Score) {

        SimilarPostResult toResult(double score) {
            return new SimilarPostResult(
                    post.getId(),
                    post.getTitle(),
                    post.getCategory(),
                    post.getContent(),
                    score);
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
