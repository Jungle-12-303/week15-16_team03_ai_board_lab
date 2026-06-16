package com.jungle_choi.namanmu.service.rag;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jungle_choi.namanmu.config.OpenAiProperties;
import com.jungle_choi.namanmu.config.RagSearchProperties;
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

    private static final TypeReference<List<Double>> EMBEDDING_VECTOR_TYPE = new TypeReference<>() {
    };

    private final PostEmbeddingRepository postEmbeddingRepository;
    private final PostEmbeddingChunkRepository postEmbeddingChunkRepository;
    private final PostTagRepository postTagRepository;
    private final OpenAiProperties openAiProperties;
    private final ObjectMapper objectMapper;
    private final KoreanTextAnalyzer koreanTextAnalyzer;
    private final QdrantVectorStoreClient qdrantVectorStoreClient;
    private final RagSearchProperties ragSearchProperties;

    public SimilarPostSearchService(
            PostEmbeddingRepository postEmbeddingRepository,
            PostEmbeddingChunkRepository postEmbeddingChunkRepository,
            PostTagRepository postTagRepository,
            OpenAiProperties openAiProperties,
            ObjectMapper objectMapper,
            KoreanTextAnalyzer koreanTextAnalyzer,
            QdrantVectorStoreClient qdrantVectorStoreClient,
            RagSearchProperties ragSearchProperties) {
        this.postEmbeddingRepository = postEmbeddingRepository;
        this.postEmbeddingChunkRepository = postEmbeddingChunkRepository;
        this.postTagRepository = postTagRepository;
        this.openAiProperties = openAiProperties;
        this.objectMapper = objectMapper;
        this.koreanTextAnalyzer = koreanTextAnalyzer;
        this.qdrantVectorStoreClient = qdrantVectorStoreClient;
        this.ragSearchProperties = ragSearchProperties;
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
        List<String> guardTerms = buildGuardTerms(title, content, tags);
        SearchMetadata metadata = ragSearchProperties.metadataEnabled()
                ? SearchMetadata.from(category, tags)
                : SearchMetadata.empty();
        Map<Long, SimilarPostResult> bestResultsByPostId = new HashMap<>();
        Map<Long, List<String>> tagNamesByPostId = new HashMap<>();
        Optional<List<Long>> qdrantChunkPostIds = qdrantVectorStoreClient.searchChunkPostIds(
                queryEmbedding,
                openAiProperties.embeddingModel());
        Optional<List<Long>> qdrantPostIds = qdrantVectorStoreClient.searchPostIds(
                queryEmbedding,
                openAiProperties.embeddingModel());

        for (SearchMetadata searchStage : metadata.relaxationStages()) {
            List<PostEmbeddingChunk> chunkCandidates =
                    findChunkCandidates(
                            qdrantChunkPostIds,
                            searchStage,
                            excludedPostId,
                            tagNamesByPostId);
            List<PostEmbedding> candidates =
                    findPostCandidates(
                            qdrantPostIds,
                            searchStage,
                            excludedPostId,
                            tagNamesByPostId);
            List<SimilarPostResult> stageResults = searchSimilarPostsByCandidates(
                    queryEmbedding,
                    ragSearchProperties.normalizedMaxLimit(),
                    queryTerms,
                    guardTerms,
                    chunkCandidates,
                    candidates,
                    tagNamesByPostId);

            for (SimilarPostResult result : stageResults) {
                bestResultsByPostId.merge(
                        result.postId(),
                        result,
                        (current, next) -> current.score() >= next.score() ? current : next);
            }
        }

        return bestResultsByPostId.values()
                .stream()
                .sorted(Comparator.comparingDouble(SimilarPostResult::score).reversed()
                        .thenComparing(SimilarPostResult::postId))
                .limit(normalizedLimit)
                .toList();
    }

    private List<SimilarPostResult> searchSimilarPostsByCandidates(
            List<Double> queryEmbedding,
            int normalizedLimit,
            List<String> queryTerms,
            List<String> guardTerms,
            List<PostEmbeddingChunk> chunkCandidates,
            List<PostEmbedding> candidates,
            Map<Long, List<String>> tagNamesByPostId) {
        Bm25CorpusStats postBm25CorpusStats = Bm25CorpusStats.from(
                candidates.stream()
                        .map((postEmbedding) -> buildDocumentTerms(
                                postEmbedding.getPost(),
                                loadTagNames(postEmbedding.getPost(), tagNamesByPostId)))
                        .toList(),
                queryTerms);

        List<ScoredCandidate> postScoredCandidates = new ArrayList<>();
        candidates.stream()
                .map((postEmbedding) -> toScoredCandidate(
                        postEmbedding,
                        queryEmbedding,
                        queryTerms,
                        guardTerms,
                        postBm25CorpusStats,
                        tagNamesByPostId))
                .flatMap(Optional::stream)
                .forEach(postScoredCandidates::add);

        List<SimilarPostResult> postResults =
                rankCandidates(postScoredCandidates, !queryTerms.isEmpty());
        Map<Long, Double> chunkScoresByPostId = scoreChunksByPostId(
                chunkCandidates,
                queryEmbedding,
                queryTerms,
                guardTerms,
                tagNamesByPostId);

        return postResults.stream()
                .map((result) -> applyChunkEvidenceBoost(result, chunkScoresByPostId))
                .filter((result) -> passesResultScoreThreshold(result, queryTerms))
                .sorted(Comparator.comparingDouble(SimilarPostResult::score).reversed()
                        .thenComparing(SimilarPostResult::postId))
                .limit(normalizedLimit)
                .toList();
    }

    private Map<Long, Double> scoreChunksByPostId(
            List<PostEmbeddingChunk> chunkCandidates,
            List<Double> queryEmbedding,
            List<String> queryTerms,
            List<String> guardTerms,
            Map<Long, List<String>> tagNamesByPostId) {
        if (chunkCandidates.isEmpty()) {
            return Map.of();
        }

        Bm25CorpusStats chunkBm25CorpusStats = Bm25CorpusStats.from(
                chunkCandidates.stream()
                        .map((chunk) -> buildChunkDocumentTerms(
                                chunk,
                                loadTagNames(chunk.getPost(), tagNamesByPostId)))
                        .toList(),
                queryTerms);

        List<ScoredCandidate> chunkScoredCandidates = chunkCandidates.stream()
                .map((chunk) -> toScoredChunkCandidate(
                        chunk,
                        queryEmbedding,
                        queryTerms,
                        guardTerms,
                        chunkBm25CorpusStats,
                        tagNamesByPostId))
                .flatMap(Optional::stream)
                .toList();

        return aggregateBestCandidatePerPost(rankCandidates(chunkScoredCandidates, !queryTerms.isEmpty()))
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        SimilarPostResult::postId,
                        SimilarPostResult::score,
                        Math::max));
    }

    private SimilarPostResult applyChunkEvidenceBoost(
            SimilarPostResult postResult,
            Map<Long, Double> chunkScoresByPostId) {
        Double chunkScore = chunkScoresByPostId.get(postResult.postId());
        if (chunkScore == null) {
            return postResult;
        }

        double chunkEvidenceWeight = ragSearchProperties.normalizedChunkEvidenceWeight();
        double boostedScore = (postResult.score() * (1.0 - chunkEvidenceWeight))
                + (chunkScore * chunkEvidenceWeight);

        return new SimilarPostResult(
                postResult.postId(),
                postResult.title(),
                postResult.category(),
                postResult.content(),
                boostedScore,
                postResult.scoreBreakdown().withChunkScore(chunkScore));
    }

    private boolean passesResultScoreThreshold(
            SimilarPostResult result,
            List<String> queryTerms) {
        if (queryTerms.isEmpty()) {
            return true;
        }

        return result.score() >= ragSearchProperties.minRankedResultScore();
    }

    private List<PostEmbeddingChunk> findChunkCandidates(
            Optional<List<Long>> qdrantPostIds,
            SearchMetadata metadata,
            Long excludedPostId,
            Map<Long, List<String>> tagNamesByPostId) {
        List<PostEmbeddingChunk> chunks = qdrantPostIds
                .map((postIds) -> postIds.isEmpty()
                        ? List.<PostEmbeddingChunk>of()
                        : postEmbeddingChunkRepository.findAllByEmbeddingModelAndPost_IdIn(
                                openAiProperties.embeddingModel(),
                                postIds))
                .orElseGet(() -> postEmbeddingChunkRepository.findAllByEmbeddingModel(
                        openAiProperties.embeddingModel()));

        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }

        return chunks.stream()
                .filter((chunk) -> isSearchCandidate(
                        chunk,
                        metadata,
                        excludedPostId,
                        tagNamesByPostId))
                .toList();
    }

    private List<PostEmbedding> findPostCandidates(
            Optional<List<Long>> qdrantPostIds,
            SearchMetadata metadata,
            Long excludedPostId,
            Map<Long, List<String>> tagNamesByPostId) {
        List<PostEmbedding> embeddings = qdrantPostIds
                .map((postIds) -> postIds.isEmpty()
                        ? List.<PostEmbedding>of()
                        : postEmbeddingRepository.findAllByEmbeddingModelAndPost_IdIn(
                                openAiProperties.embeddingModel(),
                                postIds))
                .orElseGet(() -> postEmbeddingRepository.findAllByEmbeddingModel(
                        openAiProperties.embeddingModel()));

        if (embeddings == null || embeddings.isEmpty()) {
            return List.of();
        }

        return embeddings.stream()
                .filter((postEmbedding) -> isSearchCandidate(
                        postEmbedding,
                        metadata,
                        excludedPostId,
                        tagNamesByPostId))
                .toList();
    }

    private boolean isSearchCandidate(
            PostEmbedding postEmbedding,
            SearchMetadata metadata,
            Long excludedPostId,
            Map<Long, List<String>> tagNamesByPostId) {
        return isSearchCandidate(
                postEmbedding.getPost(),
                metadata,
                excludedPostId,
                tagNamesByPostId);
    }

    private boolean isSearchCandidate(
            PostEmbeddingChunk postEmbeddingChunk,
            SearchMetadata metadata,
            Long excludedPostId,
            Map<Long, List<String>> tagNamesByPostId) {
        return isSearchCandidate(
                postEmbeddingChunk.getPost(),
                metadata,
                excludedPostId,
                tagNamesByPostId);
    }

    private boolean isSearchCandidate(
            Post post,
            SearchMetadata metadata,
            Long excludedPostId,
            Map<Long, List<String>> tagNamesByPostId) {
        if (post.getStatus() != PostStatus.PUBLISHED) {
            return false;
        }

        if (Objects.equals(post.getId(), excludedPostId)) {
            return false;
        }

        return matchesMetadata(post, metadata, tagNamesByPostId);
    }

    private Optional<ScoredCandidate> toScoredCandidate(
            PostEmbedding postEmbedding,
            List<Double> queryEmbedding,
            List<String> queryTerms,
            List<String> guardTerms,
            Bm25CorpusStats bm25CorpusStats,
            Map<Long, List<String>> tagNamesByPostId) {
        Post post = postEmbedding.getPost();
        List<String> tagNames = loadTagNames(post, tagNamesByPostId);
        List<String> documentTerms = buildDocumentTerms(post, tagNames);
        double keywordAlignmentScore = keywordAlignmentScore(
                buildCandidateGuardTerms(post, tagNames),
                guardTerms);
        List<String> matchedTerms = matchedTerms(documentTerms, queryTerms);

        List<Double> storedEmbedding = parseEmbedding(postEmbedding);
        if (storedEmbedding.size() != queryEmbedding.size()) {
            return Optional.empty();
        }

        double vectorScore = cosineSimilarity(queryEmbedding, storedEmbedding);
        double bm25Score = 0.0;
        if (!queryTerms.isEmpty()) {
            bm25Score = bm25Score(documentTerms, queryTerms, bm25CorpusStats);
        }

        if (!passesRelevanceThreshold(vectorScore, bm25Score, queryTerms)) {
            return Optional.empty();
        }

        return Optional.of(new ScoredCandidate(
                "post:%d".formatted(post.getId()),
                post,
                vectorScore,
                bm25Score,
                keywordAlignmentScore,
                !guardTerms.isEmpty(),
                matchedTerms));
    }

    private Optional<ScoredCandidate> toScoredChunkCandidate(
            PostEmbeddingChunk postEmbeddingChunk,
            List<Double> queryEmbedding,
            List<String> queryTerms,
            List<String> guardTerms,
            Bm25CorpusStats bm25CorpusStats,
            Map<Long, List<String>> tagNamesByPostId) {
        Post post = postEmbeddingChunk.getPost();
        List<String> tagNames = loadTagNames(post, tagNamesByPostId);
        List<String> documentTerms = buildChunkDocumentTerms(postEmbeddingChunk, tagNames);
        double keywordAlignmentScore = keywordAlignmentScore(
                buildCandidateGuardTerms(post, tagNames),
                guardTerms);
        List<String> matchedTerms = matchedTerms(documentTerms, queryTerms);

        List<Double> storedEmbedding = parseEmbedding(postEmbeddingChunk.getEmbeddingJson());
        if (storedEmbedding.size() != queryEmbedding.size()) {
            return Optional.empty();
        }

        double vectorScore = cosineSimilarity(queryEmbedding, storedEmbedding);
        double bm25Score = 0.0;
        if (!queryTerms.isEmpty()) {
            bm25Score = bm25Score(
                    documentTerms,
                    queryTerms,
                    bm25CorpusStats);
        }

        if (!passesRelevanceThreshold(vectorScore, bm25Score, queryTerms)) {
            return Optional.empty();
        }

        return Optional.of(new ScoredCandidate(
                "chunk:%d".formatted(postEmbeddingChunk.getId()),
                post,
                vectorScore,
                bm25Score,
                keywordAlignmentScore,
                !guardTerms.isEmpty(),
                matchedTerms));
    }

    private boolean passesRelevanceThreshold(
            double vectorScore,
            double bm25Score,
            List<String> queryTerms) {
        if (queryTerms.isEmpty()) {
            return vectorScore >= ragSearchProperties.minVectorRelevanceScore();
        }

        double hybridRelevanceScore = Math.min(
                (vectorScore * ragSearchProperties.normalizedVectorWeight())
                        + (bm25Score * ragSearchProperties.normalizedBm25Weight()),
                1.0);

        return hybridRelevanceScore >= ragSearchProperties.minHybridRelevanceScore();
    }

    private static double keywordAlignmentScore(
            List<String> candidateTerms,
            List<String> guardTerms) {
        if (guardTerms.isEmpty() || candidateTerms.isEmpty()) {
            return 0.0;
        }

        Set<String> candidateTermSet = new HashSet<>(candidateTerms);
        long matchedTermCount = guardTerms.stream()
                .filter(candidateTermSet::contains)
                .count();

        return matchedTermCount / (double) guardTerms.size();
    }

    private static List<String> matchedTerms(
            List<String> documentTerms,
            List<String> queryTerms) {
        if (documentTerms.isEmpty() || queryTerms.isEmpty()) {
            return List.of();
        }

        Set<String> documentTermSet = new HashSet<>(documentTerms);
        return queryTerms.stream()
                .filter(documentTermSet::contains)
                .distinct()
                .toList();
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

    private double bm25Score(
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
            double bm25B = ragSearchProperties.normalizedBm25B();
            double bm25K1 = ragSearchProperties.normalizedBm25K1();
            double lengthNormalization = 1.0 - bm25B
                    + (bm25B * documentLength / corpusStats.averageDocumentLength());
            double saturatedTermFrequency =
                    (termFrequency * (bm25K1 + 1.0))
                            / (termFrequency + (bm25K1 * lengthNormalization));

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

    private List<SimilarPostResult> rankCandidates(
            List<ScoredCandidate> candidates,
            boolean useHybridFusion) {
        if (!useHybridFusion) {
            return candidates.stream()
                    .map((candidate) -> {
                        double fusionScore = candidate.vectorScore();
                        return candidate.toResult(applyKeywordAlignmentScore(
                                        fusionScore,
                                        candidate.keywordAlignmentScore(),
                                        candidate.keywordAlignmentEnabled()),
                                fusionScore);
                    })
                    .toList();
        }

        if (ragSearchProperties.normalizedFusionMode() == RagSearchProperties.FusionMode.WEIGHTED) {
            return candidates.stream()
                    .map((candidate) -> {
                        double fusionScore = weightedHybridScore(candidate);
                        return candidate.toResult(applyKeywordAlignmentScore(
                                        fusionScore,
                                        candidate.keywordAlignmentScore(),
                                        candidate.keywordAlignmentEnabled()),
                                fusionScore);
                    })
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
                .map((candidate) -> {
                    double fusionScore = rrfScore(candidate, vectorRanks, bm25Ranks);
                    return candidate.toResult(applyKeywordAlignmentScore(
                                    fusionScore,
                                    candidate.keywordAlignmentScore(),
                                    candidate.keywordAlignmentEnabled()),
                            fusionScore);
                })
                .toList();
    }

    private double weightedHybridScore(ScoredCandidate candidate) {
        double weightSum = ragSearchProperties.normalizedVectorWeight()
                + ragSearchProperties.normalizedBm25Weight();
        if (weightSum == 0.0) {
            return 0.0;
        }

        return Math.min(
                ((candidate.vectorScore() * ragSearchProperties.normalizedVectorWeight())
                        + (candidate.bm25Score() * ragSearchProperties.normalizedBm25Weight()))
                        / weightSum,
                1.0);
    }

    private double applyKeywordAlignmentScore(
            double baseScore,
            double keywordAlignmentScore,
            boolean keywordAlignmentEnabled) {
        if (!keywordAlignmentEnabled) {
            return baseScore;
        }

        return Math.min(
                (baseScore * (1.0 - ragSearchProperties.normalizedKeywordAlignmentWeight()))
                        + (keywordAlignmentScore * ragSearchProperties.normalizedKeywordAlignmentWeight()),
                1.0);
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

    private double rrfScore(
            ScoredCandidate candidate,
            Map<String, Integer> vectorRanks,
            Map<String, Integer> bm25Ranks) {
        String rankId = candidate.rankId();
        double rawScore = reciprocalRankScore(vectorRanks.get(rankId))
                + reciprocalRankScore(bm25Ranks.get(rankId));
        double maxPossibleScore = 2.0 / (ragSearchProperties.normalizedRrfRankConstant() + 1.0);

        return Math.min(rawScore / maxPossibleScore, 1.0);
    }

    private double reciprocalRankScore(Integer rank) {
        if (rank == null) {
            return 0.0;
        }

        return 1.0 / (ragSearchProperties.normalizedRrfRankConstant() + rank);
    }

    private boolean matchesMetadata(
            Post post,
            SearchMetadata metadata,
            Map<Long, List<String>> tagNamesByPostId) {
        if (!metadata.hasFilters()) {
            return true;
        }

        if (!metadata.category().isBlank()
                && !KoreanTextAnalyzer.normalizeSearchText(post.getCategory()).equals(metadata.category())) {
            return false;
        }

        if (metadata.tags().isEmpty()) {
            return true;
        }

        Set<String> postTags = loadTagNames(post, tagNamesByPostId)
                .stream()
                .map(KoreanTextAnalyzer::normalizeSearchText)
                .collect(java.util.stream.Collectors.toSet());

        return metadata.tags().stream().anyMatch(postTags::contains);
    }

    private List<String> buildQueryTerms(String title, String content, List<String> tags) {
        Set<String> terms = new LinkedHashSet<>();
        addTerms(terms, title);
        addTerms(terms, tags == null ? "" : String.join(" ", tags));
        addTerms(terms, content);

        return terms.stream()
                .limit(ragSearchProperties.normalizedMaxQueryTerms())
                .toList();
    }

    private List<String> buildGuardTerms(String title, String content, List<String> tags) {
        Set<String> primaryTerms = new LinkedHashSet<>();
        addTerms(primaryTerms, title);
        addTerms(primaryTerms, tags == null ? "" : String.join(" ", tags));

        List<String> guardTerms = toGuardTerms(primaryTerms);

        if (!guardTerms.isEmpty()) {
            return guardTerms;
        }

        return toGuardTerms(new LinkedHashSet<>(koreanTextAnalyzer.tokenize(content)));
    }

    private List<String> toGuardTerms(Set<String> terms) {
        return terms.stream()
                .filter((term) -> !KoreanTextAnalyzer.isGenericGuardTerm(term))
                .limit(ragSearchProperties.normalizedMaxGuardTerms())
                .toList();
    }

    private void addTerms(Set<String> terms, String text) {
        terms.addAll(koreanTextAnalyzer.tokenize(text));
    }

    private List<String> loadTagNames(Post post, Map<Long, List<String>> tagNamesByPostId) {
        Long postId = post.getId();
        if (postId == null) {
            return readTagNames(post);
        }

        return tagNamesByPostId.computeIfAbsent(postId, (ignored) -> readTagNames(post));
    }

    private List<String> readTagNames(Post post) {
        return postTagRepository.findAllByPostIdOrderByTagNameAsc(post.getId())
                .stream()
                .map((postTag) -> postTag.getTag().getName())
                .toList();
    }

    private List<String> buildDocumentTerms(Post post, List<String> tags) {
        String joinedTags = tags == null ? "" : String.join(" ", tags);

        return koreanTextAnalyzer.tokenize("%s %s %s %s".formatted(
                post.getTitle(),
                post.getTitle(),
                post.getCategory(),
                "%s %s".formatted(joinedTags, post.getContent())));
    }

    private List<String> buildCandidateGuardTerms(Post post, List<String> tags) {
        String joinedTags = tags == null ? "" : String.join(" ", tags);

        return koreanTextAnalyzer.tokenize("%s %s %s".formatted(
                post.getTitle(),
                joinedTags,
                post.getCategory()));
    }

    private List<String> buildChunkDocumentTerms(PostEmbeddingChunk chunk, List<String> tags) {
        Post post = chunk.getPost();
        String joinedTags = tags == null ? "" : String.join(" ", tags);

        return koreanTextAnalyzer.tokenize("%s %s %s %s %s".formatted(
                post.getTitle(),
                post.getTitle(),
                joinedTags,
                post.getCategory(),
                chunk.getChunkText()));
    }

    private int normalizeLimit(int limit) {
        return Math.min(Math.max(limit, 1), ragSearchProperties.normalizedMaxLimit());
    }

    private record SearchMetadata(String category, Set<String> tags) {

        static SearchMetadata from(String category, List<String> tags) {
            String normalizedCategory = KoreanTextAnalyzer.normalizeSearchText(category);
            if ("all".equals(normalizedCategory)) {
                normalizedCategory = "";
            }

            Set<String> normalizedTags = tags == null
                    ? Set.of()
                    : tags.stream()
                            .map(KoreanTextAnalyzer::normalizeSearchText)
                            .filter((tag) -> !tag.isBlank())
                            .filter((tag) -> !isGenericMetadataTag(tag))
                            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

            return new SearchMetadata(normalizedCategory, normalizedTags);
        }

        static SearchMetadata empty() {
            return new SearchMetadata("", Set.of());
        }

        boolean hasFilters() {
            return !category.isBlank() || !tags.isEmpty();
        }

        List<SearchMetadata> relaxationStages() {
            List<SearchMetadata> stages = new ArrayList<>();
            stages.add(this);

            if (!tags.isEmpty()) {
                stages.add(new SearchMetadata(category, Set.of()));
            }

            if (!category.isBlank()) {
                stages.add(new SearchMetadata("", tags));
            }

            if (hasFilters()) {
                stages.add(new SearchMetadata("", Set.of()));
            }

            return stages.stream()
                    .distinct()
                    .toList();
        }
    }

    private static boolean isGenericMetadataTag(String tag) {
        return Set.of(
                "daily",
                "learning",
                "project",
                "development",
                "review",
                "briefing")
                .contains(tag);
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
            double bm25Score,
            double keywordAlignmentScore,
            boolean keywordAlignmentEnabled,
            List<String> matchedTerms) {

        SimilarPostResult toResult(double score, double fusionScore) {
            return new SimilarPostResult(
                    post.getId(),
                    post.getTitle(),
                    post.getCategory(),
                    post.getContent(),
                    score,
                    new SearchScoreBreakdown(
                            vectorScore,
                            bm25Score,
                            fusionScore,
                            keywordAlignmentScore,
                            0.0,
                            matchedTerms));
        }
    }

    public record SimilarPostResult(
            Long postId,
            String title,
            String category,
            String content,
            double score,
            SearchScoreBreakdown scoreBreakdown) {
    }

    public record SearchScoreBreakdown(
            double vectorScore,
            double bm25Score,
            double fusionScore,
            double keywordAlignmentScore,
            double chunkScore,
            List<String> matchedTerms) {

        SearchScoreBreakdown withChunkScore(double nextChunkScore) {
            return new SearchScoreBreakdown(
                    vectorScore,
                    bm25Score,
                    fusionScore,
                    keywordAlignmentScore,
                    nextChunkScore,
                    matchedTerms);
        }
    }
}
