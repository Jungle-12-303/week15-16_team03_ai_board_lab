package com.jungle_choi.namanmu.service.rag;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jungle_choi.namanmu.config.OpenAiProperties;
import com.jungle_choi.namanmu.domain.embedding.PostEmbedding;
import com.jungle_choi.namanmu.domain.embedding.PostEmbeddingChunk;
import com.jungle_choi.namanmu.domain.embedding.PostEmbeddingChunkRepository;
import com.jungle_choi.namanmu.domain.embedding.PostEmbeddingRepository;
import com.jungle_choi.namanmu.domain.post.Post;
import com.jungle_choi.namanmu.domain.post.PostRepository;
import com.jungle_choi.namanmu.domain.post.PostStatus;
import com.jungle_choi.namanmu.domain.post.PostTag;
import com.jungle_choi.namanmu.domain.post.PostTagRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RagAblationEvaluationService {

    private static final int DEFAULT_EVALUATION_LIMIT = 5;
    private static final int MAX_EVALUATION_LIMIT = 10;
    private static final double MIN_VECTOR_RELEVANCE_SCORE = 0.45;
    private static final double MIN_HYBRID_RELEVANCE_SCORE = 0.38;
    private static final double MIN_RANKED_RESULT_SCORE = 0.60;
    private static final double VECTOR_WEIGHT_WITH_QUERY_TERMS = 0.35;
    private static final double BM25_WEIGHT_WITH_QUERY_TERMS = 0.65;
    private static final double KEYWORD_ALIGNMENT_WEIGHT = 0.25;
    private static final double CHUNK_EVIDENCE_WEIGHT = 0.01;
    private static final double BM25_K1 = 1.2;
    private static final double BM25_B = 0.75;
    private static final double RRF_RANK_CONSTANT = 60.0;
    private static final int MAX_QUERY_TERMS = 16;
    private static final int MAX_GUARD_TERMS = 8;
    private static final TypeReference<List<Double>> EMBEDDING_VECTOR_TYPE = new TypeReference<>() {
    };

    private final PostEmbeddingTextBuilder postEmbeddingTextBuilder;
    private final OpenAiEmbeddingClient openAiEmbeddingClient;
    private final SimilarPostSearchService similarPostSearchService;
    private final PostRepository postRepository;
    private final PostTagRepository postTagRepository;
    private final PostEmbeddingRepository postEmbeddingRepository;
    private final PostEmbeddingChunkRepository postEmbeddingChunkRepository;
    private final OpenAiProperties openAiProperties;
    private final ObjectMapper objectMapper;
    private final KoreanTextAnalyzer koreanTextAnalyzer;

    public RagAblationEvaluationService(
            PostEmbeddingTextBuilder postEmbeddingTextBuilder,
            OpenAiEmbeddingClient openAiEmbeddingClient,
            SimilarPostSearchService similarPostSearchService,
            PostRepository postRepository,
            PostTagRepository postTagRepository,
            PostEmbeddingRepository postEmbeddingRepository,
            PostEmbeddingChunkRepository postEmbeddingChunkRepository,
            OpenAiProperties openAiProperties,
            ObjectMapper objectMapper,
            KoreanTextAnalyzer koreanTextAnalyzer) {
        this.postEmbeddingTextBuilder = postEmbeddingTextBuilder;
        this.openAiEmbeddingClient = openAiEmbeddingClient;
        this.similarPostSearchService = similarPostSearchService;
        this.postRepository = postRepository;
        this.postTagRepository = postTagRepository;
        this.postEmbeddingRepository = postEmbeddingRepository;
        this.postEmbeddingChunkRepository = postEmbeddingChunkRepository;
        this.openAiProperties = openAiProperties;
        this.objectMapper = objectMapper;
        this.koreanTextAnalyzer = koreanTextAnalyzer;
    }

    @Transactional(readOnly = true)
    public AblationEvaluationReport evaluateVariants(int requestedLimit) {
        int limit = normalizeLimit(requestedLimit);
        List<Post> publishedPosts = postRepository.findAll()
                .stream()
                .filter((post) -> post.getStatus() == PostStatus.PUBLISHED)
                .toList();
        Map<Long, List<String>> tagNamesByPostId = loadTagNamesByPostId(publishedPosts);
        List<PostDocument> postDocuments = loadPostDocuments(tagNamesByPostId);
        List<ChunkDocument> chunkDocuments = loadChunkDocuments(tagNamesByPostId);
        List<Variant> variants = variants();
        Map<String, List<AblationCaseResult>> caseResultsByVariant = new LinkedHashMap<>();

        variants.forEach((variant) -> caseResultsByVariant.put(variant.id(), new ArrayList<>()));

        for (RagEvaluationCase evaluationCase : evaluationCases()) {
            String queryText = postEmbeddingTextBuilder.buildQuery(
                    evaluationCase.category(),
                    evaluationCase.title(),
                    evaluationCase.content(),
                    evaluationCase.tags());
            OpenAiEmbeddingClient.EmbeddingResult embeddingResult =
                    openAiEmbeddingClient.createEmbedding(queryText);
            QuerySignals querySignals = QuerySignals.from(
                    evaluationCase,
                    embeddingResult.embedding(),
                    koreanTextAnalyzer);
            int relevantTotal = (int) publishedPosts.stream()
                    .filter((post) -> isRelevant(
                            evaluationCase,
                            post,
                            tagNamesByPostId.getOrDefault(post.getId(), List.of())))
                    .count();

            for (Variant variant : variants) {
                List<AblationSearchResult> searchResults = variant.production()
                        ? searchProduction(evaluationCase, embeddingResult.embedding(), limit)
                        : searchAblation(
                                variant,
                                querySignals,
                                postDocuments,
                                chunkDocuments,
                                limit);

                caseResultsByVariant.get(variant.id())
                        .add(toCaseResult(
                                evaluationCase,
                                relevantTotal,
                                searchResults,
                                limit));
            }
        }

        List<AblationVariantResult> variantResults = variants.stream()
                .map((variant) -> toVariantResult(variant, caseResultsByVariant.get(variant.id())))
                .toList();

        return new AblationEvaluationReport(
                limit,
                postDocuments.size(),
                chunkDocuments.size(),
                openAiProperties.embeddingModel(),
                variantResults);
    }

    private List<AblationSearchResult> searchProduction(
            RagEvaluationCase evaluationCase,
            List<Double> queryEmbedding,
            int limit) {
        return similarPostSearchService.searchSimilarPosts(
                        queryEmbedding,
                        null,
                        limit,
                        evaluationCase.category(),
                        evaluationCase.title(),
                        evaluationCase.content(),
                        evaluationCase.tags())
                .stream()
                .map((result) -> new AblationSearchResult(
                        result.postId(),
                        result.title(),
                        result.category(),
                        result.content(),
                        result.score()))
                .toList();
    }

    private List<AblationSearchResult> searchAblation(
            Variant variant,
            QuerySignals querySignals,
            List<PostDocument> postDocuments,
            List<ChunkDocument> chunkDocuments,
            int limit) {
        List<PostDocument> candidates = filterByMetadataIfNeeded(variant, querySignals, postDocuments);
        Bm25CorpusStats postBm25Stats = Bm25CorpusStats.from(
                candidates.stream()
                        .map(PostDocument::documentTerms)
                        .toList(),
                querySignals.queryTerms());
        List<ScoredDocument> scoredDocuments = candidates.stream()
                .map((document) -> scorePostDocument(variant, querySignals, document, postBm25Stats))
                .filter((scoredDocument) -> passesThreshold(variant, scoredDocument, querySignals))
                .toList();
        Map<Long, Double> chunkScoresByPostId = variant.useChunks()
                ? scoreChunksByPostId(variant, querySignals, chunkDocuments)
                : Map.of();

        return rankDocuments(variant, scoredDocuments)
                .stream()
                .map((result) -> applyChunkEvidenceIfNeeded(variant, result, chunkScoresByPostId))
                .filter((result) -> passesRankedResultThreshold(variant, result, querySignals))
                .sorted(Comparator.comparingDouble(AblationSearchResult::score).reversed()
                        .thenComparing(AblationSearchResult::postId))
                .limit(limit)
                .toList();
    }

    private List<PostDocument> filterByMetadataIfNeeded(
            Variant variant,
            QuerySignals querySignals,
            List<PostDocument> postDocuments) {
        if (!variant.useMetadata() || !querySignals.metadata().hasFilters()) {
            return postDocuments;
        }

        for (SearchMetadata metadata : querySignals.metadata().relaxationStages()) {
            List<PostDocument> filtered = postDocuments.stream()
                    .filter((document) -> metadata.matches(document))
                    .toList();
            if (!filtered.isEmpty()) {
                return filtered;
            }
        }

        return postDocuments;
    }

    private ScoredDocument scorePostDocument(
            Variant variant,
            QuerySignals querySignals,
            PostDocument document,
            Bm25CorpusStats bm25CorpusStats) {
        double vectorScore = SimilarPostSearchService.cosineSimilarity(
                querySignals.queryEmbedding(),
                document.embedding());
        double bm25Score = variant.useBm25()
                ? bm25Score(document.documentTerms(), querySignals.queryTerms(), bm25CorpusStats)
                : 0.0;
        double keywordAlignmentScore = variant.useKeywordAlignment()
                ? keywordAlignmentScore(document.guardTerms(), querySignals.guardTerms())
                : 0.0;

        return new ScoredDocument(
                document,
                vectorScore,
                bm25Score,
                keywordAlignmentScore);
    }

    private Map<Long, Double> scoreChunksByPostId(
            Variant variant,
            QuerySignals querySignals,
            List<ChunkDocument> chunkDocuments) {
        if (chunkDocuments.isEmpty()) {
            return Map.of();
        }

        List<ChunkDocument> filteredChunks = chunkDocuments.stream()
                .filter((chunk) -> !variant.useMetadata() || querySignals.metadata().matches(chunk.postDocument()))
                .toList();
        Bm25CorpusStats chunkBm25Stats = Bm25CorpusStats.from(
                filteredChunks.stream()
                        .map(ChunkDocument::documentTerms)
                        .toList(),
                querySignals.queryTerms());
        List<ScoredDocument> scoredChunks = filteredChunks.stream()
                .map((chunk) -> scoreChunkDocument(variant, querySignals, chunk, chunkBm25Stats))
                .filter((scoredChunk) -> passesThreshold(variant, scoredChunk, querySignals))
                .toList();

        Map<Long, Double> bestChunkScoreByPostId = new HashMap<>();
        rankDocuments(variant, scoredChunks).forEach((result) ->
                bestChunkScoreByPostId.merge(result.postId(), result.score(), Math::max));

        return bestChunkScoreByPostId;
    }

    private ScoredDocument scoreChunkDocument(
            Variant variant,
            QuerySignals querySignals,
            ChunkDocument chunk,
            Bm25CorpusStats bm25CorpusStats) {
        double vectorScore = SimilarPostSearchService.cosineSimilarity(
                querySignals.queryEmbedding(),
                chunk.embedding());
        double bm25Score = variant.useBm25()
                ? bm25Score(chunk.documentTerms(), querySignals.queryTerms(), bm25CorpusStats)
                : 0.0;
        double keywordAlignmentScore = variant.useKeywordAlignment()
                ? keywordAlignmentScore(chunk.postDocument().guardTerms(), querySignals.guardTerms())
                : 0.0;

        return new ScoredDocument(
                chunk.postDocument(),
                vectorScore,
                bm25Score,
                keywordAlignmentScore);
    }

    private List<AblationSearchResult> rankDocuments(
            Variant variant,
            List<ScoredDocument> scoredDocuments) {
        if (!variant.useRrf()) {
            return scoredDocuments.stream()
                    .map((scoredDocument) -> toSearchResult(
                            variant,
                            scoredDocument,
                            weightedScore(variant, scoredDocument),
                            weightedScore(variant, scoredDocument)))
                    .toList();
        }

        Map<Long, Integer> vectorRanks = rankBy(
                scoredDocuments,
                ScoredDocument::vectorScore,
                (scoredDocument) -> scoredDocument.vectorScore() > 0.0);
        Map<Long, Integer> bm25Ranks = rankBy(
                scoredDocuments,
                ScoredDocument::bm25Score,
                (scoredDocument) -> scoredDocument.bm25Score() > 0.0);

        return scoredDocuments.stream()
                .map((scoredDocument) -> {
                    double fusionScore = rrfScore(scoredDocument, vectorRanks, bm25Ranks);
                    return toSearchResult(variant, scoredDocument, fusionScore, fusionScore);
                })
                .toList();
    }

    private AblationSearchResult toSearchResult(
            Variant variant,
            ScoredDocument scoredDocument,
            double baseScore,
            double fusionScore) {
        PostDocument document = scoredDocument.document();
        double score = variant.useKeywordAlignment()
                ? applyKeywordAlignmentScore(baseScore, scoredDocument.keywordAlignmentScore())
                : baseScore;

        return new AblationSearchResult(
                document.postId(),
                document.title(),
                document.category(),
                document.content(),
                Math.min(score, 1.0));
    }

    private AblationSearchResult applyChunkEvidenceIfNeeded(
            Variant variant,
            AblationSearchResult result,
            Map<Long, Double> chunkScoresByPostId) {
        if (!variant.useChunks()) {
            return result;
        }

        Double chunkScore = chunkScoresByPostId.get(result.postId());
        if (chunkScore == null) {
            return result;
        }

        return new AblationSearchResult(
                result.postId(),
                result.title(),
                result.category(),
                result.content(),
                (result.score() * (1.0 - CHUNK_EVIDENCE_WEIGHT)) + (chunkScore * CHUNK_EVIDENCE_WEIGHT));
    }

    private static boolean passesThreshold(
            Variant variant,
            ScoredDocument scoredDocument,
            QuerySignals querySignals) {
        if (!variant.useThreshold()) {
            return true;
        }

        if (querySignals.queryTerms().isEmpty() || !variant.useBm25()) {
            return scoredDocument.vectorScore() >= MIN_VECTOR_RELEVANCE_SCORE;
        }

        double hybridScore = Math.min(
                (scoredDocument.vectorScore() * VECTOR_WEIGHT_WITH_QUERY_TERMS)
                        + (scoredDocument.bm25Score() * BM25_WEIGHT_WITH_QUERY_TERMS),
                1.0);

        return hybridScore >= MIN_HYBRID_RELEVANCE_SCORE;
    }

    private static boolean passesRankedResultThreshold(
            Variant variant,
            AblationSearchResult result,
            QuerySignals querySignals) {
        if (!variant.useRankedThreshold() || querySignals.queryTerms().isEmpty()) {
            return true;
        }

        return result.score() >= MIN_RANKED_RESULT_SCORE;
    }

    private static double weightedScore(Variant variant, ScoredDocument scoredDocument) {
        if (!variant.useBm25()) {
            return scoredDocument.vectorScore();
        }

        return Math.min(
                (scoredDocument.vectorScore() * VECTOR_WEIGHT_WITH_QUERY_TERMS)
                        + (scoredDocument.bm25Score() * BM25_WEIGHT_WITH_QUERY_TERMS),
                1.0);
    }

    private static double applyKeywordAlignmentScore(
            double baseScore,
            double keywordAlignmentScore) {
        return Math.min(
                (baseScore * (1.0 - KEYWORD_ALIGNMENT_WEIGHT))
                        + (keywordAlignmentScore * KEYWORD_ALIGNMENT_WEIGHT),
                1.0);
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

    private static Map<Long, Integer> rankBy(
            List<ScoredDocument> scoredDocuments,
            ToDoubleFunction<ScoredDocument> scoreExtractor,
            Predicate<ScoredDocument> filter) {
        List<ScoredDocument> rankedDocuments = scoredDocuments.stream()
                .filter(filter)
                .sorted((left, right) -> {
                    int scoreComparison = Double.compare(
                            scoreExtractor.applyAsDouble(right),
                            scoreExtractor.applyAsDouble(left));
                    if (scoreComparison != 0) {
                        return scoreComparison;
                    }

                    return Long.compare(left.document().postId(), right.document().postId());
                })
                .toList();
        Map<Long, Integer> ranks = new HashMap<>();

        for (int index = 0; index < rankedDocuments.size(); index++) {
            ranks.put(rankedDocuments.get(index).document().postId(), index + 1);
        }

        return ranks;
    }

    private static double rrfScore(
            ScoredDocument scoredDocument,
            Map<Long, Integer> vectorRanks,
            Map<Long, Integer> bm25Ranks) {
        Long postId = scoredDocument.document().postId();
        double rawScore = reciprocalRankScore(vectorRanks.get(postId))
                + reciprocalRankScore(bm25Ranks.get(postId));
        double maxPossibleScore = 2.0 / (RRF_RANK_CONSTANT + 1.0);

        return Math.min(rawScore / maxPossibleScore, 1.0);
    }

    private static double reciprocalRankScore(Integer rank) {
        if (rank == null) {
            return 0.0;
        }

        return 1.0 / (RRF_RANK_CONSTANT + rank);
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

    private AblationCaseResult toCaseResult(
            RagEvaluationCase evaluationCase,
            int relevantTotal,
            List<AblationSearchResult> searchResults,
            int limit) {
        int relevantRetrievedCount = 0;
        int firstRelevantRank = 0;
        double dcg = 0.0;
        List<AblationRetrievedPost> retrievedPosts = new ArrayList<>();

        for (int index = 0; index < searchResults.size(); index++) {
            AblationSearchResult result = searchResults.get(index);
            boolean relevant = isRelevant(evaluationCase, result);

            if (relevant) {
                relevantRetrievedCount++;
                if (firstRelevantRank == 0) {
                    firstRelevantRank = index + 1;
                }
                dcg += discountedGain(index + 1);
            }

            retrievedPosts.add(new AblationRetrievedPost(
                    result.postId(),
                    result.title(),
                    result.category(),
                    result.score(),
                    relevant));
        }

        double precisionAtK = relevantRetrievedCount / (double) limit;
        double recallAtK = relevantTotal == 0
                ? 0.0
                : relevantRetrievedCount / (double) relevantTotal;
        double mrrAtK = firstRelevantRank == 0
                ? 0.0
                : 1.0 / firstRelevantRank;
        double ndcgAtK = ndcgAtK(dcg, relevantTotal, limit);
        double hitAtK = firstRelevantRank == 0 ? 0.0 : 1.0;

        return new AblationCaseResult(
                evaluationCase.id(),
                relevantTotal,
                searchResults.size(),
                precisionAtK,
                recallAtK,
                mrrAtK,
                ndcgAtK,
                hitAtK,
                retrievedPosts);
    }

    private AblationVariantResult toVariantResult(
            Variant variant,
            List<AblationCaseResult> caseResults) {
        return new AblationVariantResult(
                variant.id(),
                variant.label(),
                variant.description(),
                average(caseResults.stream()
                        .mapToDouble(AblationCaseResult::precisionAtK)
                        .toArray()),
                average(caseResults.stream()
                        .mapToDouble(AblationCaseResult::recallAtK)
                        .toArray()),
                average(caseResults.stream()
                        .mapToDouble(AblationCaseResult::mrrAtK)
                        .toArray()),
                average(caseResults.stream()
                        .mapToDouble(AblationCaseResult::ndcgAtK)
                        .toArray()),
                average(caseResults.stream()
                        .mapToDouble(AblationCaseResult::hitAtK)
                        .toArray()),
                caseResults);
    }

    private boolean isRelevant(RagEvaluationCase evaluationCase, AblationSearchResult result) {
        String text = "%s %s %s".formatted(
                result.title(),
                result.category(),
                result.content());

        return isRelevant(evaluationCase, text);
    }

    private boolean isRelevant(
            RagEvaluationCase evaluationCase,
            Post post,
            List<String> tagNames) {
        String tags = String.join(" ", tagNames);
        String text = "%s %s %s %s".formatted(
                post.getTitle(),
                post.getCategory(),
                post.getContent(),
                tags);

        return isRelevant(evaluationCase, text);
    }

    private boolean isRelevant(RagEvaluationCase evaluationCase, String text) {
        Set<String> subjectTerms = extractSubjectTerms(text);
        boolean requiredTermsMatched = evaluationCase.requiredTerms()
                .stream()
                .allMatch((requiredTerm) -> containsAllSubjectTerms(subjectTerms, requiredTerm));
        long matchedAnyTermCount = evaluationCase.anyTerms()
                .stream()
                .filter((anyTerm) -> containsAllSubjectTerms(subjectTerms, anyTerm))
                .count();
        boolean anyTermsMatched = matchedAnyTermCount >= evaluationCase.minimumAnyTermMatches();

        return requiredTermsMatched && anyTermsMatched;
    }

    private boolean containsAllSubjectTerms(Set<String> subjectTerms, String expectedText) {
        Set<String> expectedTerms = extractSubjectTerms(expectedText);
        if (expectedTerms.isEmpty()) {
            return false;
        }

        return subjectTerms.containsAll(expectedTerms);
    }

    private Set<String> extractSubjectTerms(String text) {
        Set<String> subjectTerms = new LinkedHashSet<>();

        koreanTextAnalyzer.tokenize(text)
                .stream()
                .forEach(subjectTerms::add);

        return subjectTerms;
    }

    private List<PostDocument> loadPostDocuments(Map<Long, List<String>> tagNamesByPostId) {
        return postEmbeddingRepository.findAllByEmbeddingModel(openAiProperties.embeddingModel())
                .stream()
                .filter((postEmbedding) -> postEmbedding.getPost().getStatus() == PostStatus.PUBLISHED)
                .map((postEmbedding) -> {
                    Post post = postEmbedding.getPost();
                    List<String> tagNames = tagNamesByPostId.getOrDefault(post.getId(), List.of());
                    return PostDocument.from(
                            post,
                            tagNames,
                            parseEmbedding(postEmbedding.getEmbeddingJson()),
                            koreanTextAnalyzer);
                })
                .toList();
    }

    private List<ChunkDocument> loadChunkDocuments(Map<Long, List<String>> tagNamesByPostId) {
        Map<Long, PostDocument> postDocumentsByPostId = loadPostDocuments(tagNamesByPostId)
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        PostDocument::postId,
                        (postDocument) -> postDocument,
                        (left, right) -> left));

        return postEmbeddingChunkRepository.findAllByEmbeddingModel(openAiProperties.embeddingModel())
                .stream()
                .filter((chunk) -> chunk.getPost().getStatus() == PostStatus.PUBLISHED)
                .map((chunk) -> toChunkDocument(chunk, postDocumentsByPostId))
                .toList();
    }

    private ChunkDocument toChunkDocument(
            PostEmbeddingChunk chunk,
            Map<Long, PostDocument> postDocumentsByPostId) {
        Post post = chunk.getPost();
        PostDocument postDocument = postDocumentsByPostId.get(post.getId());
        if (postDocument == null) {
            List<String> tagNames = postTagRepository.findAllByPostIdOrderByTagNameAsc(post.getId())
                    .stream()
                    .map((postTag) -> postTag.getTag().getName())
                    .toList();
            postDocument = PostDocument.from(
                    post,
                    tagNames,
                    List.of(),
                    koreanTextAnalyzer);
        }

        return ChunkDocument.from(
                postDocument,
                chunk,
                parseEmbedding(chunk.getEmbeddingJson()),
                koreanTextAnalyzer);
    }

    private Map<Long, List<String>> loadTagNamesByPostId(List<Post> posts) {
        List<Long> postIds = posts.stream()
                .map(Post::getId)
                .toList();
        if (postIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<String>> tagNamesByPostId = new HashMap<>();
        for (PostTag postTag : postTagRepository.findAllByPost_IdIn(postIds)) {
            Long postId = postTag.getPost().getId();
            tagNamesByPostId
                    .computeIfAbsent(postId, (ignored) -> new ArrayList<>())
                    .add(postTag.getTag().getName());
        }

        return tagNamesByPostId;
    }

    private List<Double> parseEmbedding(String embeddingJson) {
        try {
            return objectMapper.readValue(embeddingJson, EMBEDDING_VECTOR_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored embedding vector could not be parsed.", exception);
        }
    }

    private static double ndcgAtK(double dcg, int relevantTotal, int limit) {
        int idealRelevantCount = Math.min(relevantTotal, limit);
        if (idealRelevantCount == 0) {
            return 0.0;
        }

        double idealDcg = 0.0;
        for (int rank = 1; rank <= idealRelevantCount; rank++) {
            idealDcg += discountedGain(rank);
        }

        return dcg / idealDcg;
    }

    private static double discountedGain(int rank) {
        return 1.0 / (Math.log(rank + 1.0) / Math.log(2.0));
    }

    private static int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_EVALUATION_LIMIT;
        }

        return Math.min(limit, MAX_EVALUATION_LIMIT);
    }

    private static double average(double[] values) {
        if (values.length == 0) {
            return 0.0;
        }

        double sum = 0.0;
        for (double value : values) {
            sum += value;
        }

        return sum / values.length;
    }

    private static List<Variant> variants() {
        return List.of(
                new Variant(
                        "vector-only",
                        "Vector only",
                        "게시글 전체 임베딩 코사인 유사도만 사용",
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false),
                new Variant(
                        "vector-threshold",
                        "Vector + threshold",
                        "코사인 유사도에 최소 점수 기준 적용",
                        true,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false),
                new Variant(
                        "vector-keyword",
                        "Vector + keyword guard",
                        "벡터 점수에 핵심 주제어 정렬 점수 반영",
                        true,
                        true,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false),
                new Variant(
                        "vector-bm25",
                        "Vector + BM25 weighted",
                        "벡터 점수와 BM25 단어 점수를 가중합",
                        true,
                        true,
                        true,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false),
                new Variant(
                        "hybrid-rrf",
                        "Vector + BM25 + RRF",
                        "벡터 순위와 BM25 순위를 RRF로 결합",
                        true,
                        true,
                        true,
                        true,
                        false,
                        false,
                        false,
                        true,
                        false),
                new Variant(
                        "metadata-hybrid",
                        "Hybrid + metadata",
                        "카테고리/태그 metadata 완화 필터 추가",
                        true,
                        true,
                        true,
                        true,
                        true,
                        false,
                        false,
                        true,
                        false),
                new Variant(
                        "chunk-evidence",
                        "Hybrid + metadata + chunks",
                        "게시글 전체 임베딩에 청크 근거 점수 보조 반영",
                        true,
                        true,
                        true,
                        true,
                        true,
                        true,
                        false,
                        true,
                        false),
                new Variant(
                        "final-production",
                        "Final production",
                        "Nori + Qdrant 후보 검색 + BM25/RRF/metadata/chunk 재정렬",
                        true,
                        true,
                        true,
                        true,
                        true,
                        true,
                        true,
                        true,
                        true));
    }

    private static List<RagEvaluationCase> evaluationCases() {
        return List.of(
                new RagEvaluationCase(
                        "hardware-cpu-upgrade",
                        "Daily",
                        "7800X3D에서 9800X3D로 업그레이드할지 고민",
                        "현재 7800X3D를 쓰고 있는데 CPU를 바꿀지 다음 세대를 기다릴지 고민 중입니다.",
                        List.of("하드웨어", "CPU", "업그레이드"),
                        List.of("[hw-scenario]"),
                        2,
                        List.of("cpu", "upgrade", "9800x3d", "7800x3d")),
                new RagEvaluationCase(
                        "hardware-gpu-noise",
                        "Daily",
                        "GPU 팬 소음과 언더볼팅 기록",
                        "그래픽카드 팬 소음이 거슬려서 언더볼팅과 온도 변화를 기록하려고 합니다.",
                        List.of("하드웨어", "GPU", "발열"),
                        List.of("[hw-scenario]"),
                        2,
                        List.of("gpu", "undervolt", "fan", "noise")),
                new RagEvaluationCase(
                        "github-actions-deploy",
                        "Learning",
                        "GitHub Actions 배포 실패 정리",
                        "GitHub Actions workflow에서 배포가 실패해서 secrets와 CI 설정을 정리하려고 합니다.",
                        List.of("GitHub", "CI"),
                        List.of("github"),
                        2,
                        List.of("actions", "workflow", "deploy", "ci", "secrets")),
                new RagEvaluationCase(
                        "react-state-hooks",
                        "Learning",
                        "React state와 hook 학습 메모",
                        "useState와 custom hook을 쓰면서 상태 관리가 어떻게 분리되는지 정리하려고 합니다.",
                        List.of("React", "State"),
                        List.of("react"),
                        1,
                        List.of("hook", "state", "usestate")),
                new RagEvaluationCase(
                        "weather-briefing",
                        "Briefing",
                        "오늘 날씨 브리핑 작성",
                        "오늘 지역 날씨와 기온을 짧은 게시글로 정리하려고 합니다.",
                        List.of("날씨", "브리핑"),
                        List.of("weather"),
                        1,
                        List.of("temperature", "forecast", "current", "location")));
    }

    private record Variant(
            String id,
            String label,
            String description,
            boolean useThreshold,
            boolean useKeywordAlignment,
            boolean useBm25,
            boolean useRrf,
            boolean useMetadata,
            boolean useChunks,
            boolean useQdrant,
            boolean useRankedThreshold,
            boolean production) {
    }

    private record QuerySignals(
            List<Double> queryEmbedding,
            List<String> queryTerms,
            List<String> guardTerms,
            SearchMetadata metadata) {

        static QuerySignals from(
                RagEvaluationCase evaluationCase,
                List<Double> queryEmbedding,
                KoreanTextAnalyzer koreanTextAnalyzer) {
            List<String> queryTerms = buildQueryTerms(
                    evaluationCase.title(),
                    evaluationCase.content(),
                    evaluationCase.tags(),
                    koreanTextAnalyzer);
            List<String> guardTerms = buildGuardTerms(
                    evaluationCase.title(),
                    evaluationCase.content(),
                    evaluationCase.tags(),
                    koreanTextAnalyzer);

            return new QuerySignals(
                    queryEmbedding,
                    queryTerms,
                    guardTerms,
                    SearchMetadata.from(evaluationCase.category(), evaluationCase.tags()));
        }
    }

    private static List<String> buildQueryTerms(
            String title,
            String content,
            List<String> tags,
            KoreanTextAnalyzer koreanTextAnalyzer) {
        Set<String> terms = new LinkedHashSet<>();
        addTerms(terms, title, koreanTextAnalyzer);
        addTerms(terms, tags == null ? "" : String.join(" ", tags), koreanTextAnalyzer);
        addTerms(terms, content, koreanTextAnalyzer);

        return terms.stream()
                .limit(MAX_QUERY_TERMS)
                .toList();
    }

    private static List<String> buildGuardTerms(
            String title,
            String content,
            List<String> tags,
            KoreanTextAnalyzer koreanTextAnalyzer) {
        Set<String> primaryTerms = new LinkedHashSet<>();
        addTerms(primaryTerms, title, koreanTextAnalyzer);
        addTerms(primaryTerms, tags == null ? "" : String.join(" ", tags), koreanTextAnalyzer);

        List<String> guardTerms = toGuardTerms(primaryTerms);

        if (!guardTerms.isEmpty()) {
            return guardTerms;
        }

        return toGuardTerms(new LinkedHashSet<>(koreanTextAnalyzer.tokenize(content)));
    }

    private static List<String> toGuardTerms(Set<String> terms) {
        return terms.stream()
                .filter((term) -> !KoreanTextAnalyzer.isGenericGuardTerm(term))
                .limit(MAX_GUARD_TERMS)
                .toList();
    }

    private static void addTerms(
            Set<String> terms,
            String text,
            KoreanTextAnalyzer koreanTextAnalyzer) {
        terms.addAll(koreanTextAnalyzer.tokenize(text));
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

        boolean hasFilters() {
            return !category.isBlank() || !tags.isEmpty();
        }

        boolean matches(PostDocument document) {
            if (!hasFilters()) {
                return true;
            }

            if (!category.isBlank()
                    && !KoreanTextAnalyzer.normalizeSearchText(document.category()).equals(category)) {
                return false;
            }

            if (tags.isEmpty()) {
                return true;
            }

            return tags.stream().anyMatch(document.normalizedTags()::contains);
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

    private record PostDocument(
            Long postId,
            String title,
            String category,
            String content,
            List<String> tagNames,
            Set<String> normalizedTags,
            List<String> documentTerms,
            List<String> guardTerms,
            List<Double> embedding) {

        static PostDocument from(
                Post post,
                List<String> tagNames,
                List<Double> embedding,
                KoreanTextAnalyzer koreanTextAnalyzer) {
            String joinedTags = tagNames == null ? "" : String.join(" ", tagNames);
            List<String> documentTerms = koreanTextAnalyzer.tokenize("%s %s %s %s".formatted(
                    post.getTitle(),
                    post.getTitle(),
                    post.getCategory(),
                    "%s %s".formatted(joinedTags, post.getContent())));
            List<String> guardTerms = koreanTextAnalyzer.tokenize("%s %s %s".formatted(
                    post.getTitle(),
                    joinedTags,
                    post.getCategory()));
            Set<String> normalizedTags = tagNames == null
                    ? Set.of()
                    : tagNames.stream()
                            .map(KoreanTextAnalyzer::normalizeSearchText)
                            .collect(java.util.stream.Collectors.toSet());

            return new PostDocument(
                    post.getId(),
                    post.getTitle(),
                    post.getCategory(),
                    post.getContent(),
                    tagNames == null ? List.of() : tagNames,
                    normalizedTags,
                    documentTerms,
                    guardTerms,
                    embedding);
        }
    }

    private record ChunkDocument(
            PostDocument postDocument,
            List<String> documentTerms,
            List<Double> embedding) {

        static ChunkDocument from(
                PostDocument postDocument,
                PostEmbeddingChunk chunk,
                List<Double> embedding,
                KoreanTextAnalyzer koreanTextAnalyzer) {
            String joinedTags = String.join(" ", postDocument.tagNames());
            List<String> documentTerms = koreanTextAnalyzer.tokenize("%s %s %s %s %s".formatted(
                    postDocument.title(),
                    postDocument.title(),
                    joinedTags,
                    postDocument.category(),
                    chunk.getChunkText()));

            return new ChunkDocument(postDocument, documentTerms, embedding);
        }
    }

    private record ScoredDocument(
            PostDocument document,
            double vectorScore,
            double bm25Score,
            double keywordAlignmentScore) {
    }

    private record AblationSearchResult(
            Long postId,
            String title,
            String category,
            String content,
            double score) {
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

    private record RagEvaluationCase(
            String id,
            String category,
            String title,
            String content,
            List<String> tags,
            List<String> requiredTerms,
            int minimumAnyTermMatches,
            List<String> anyTerms) {
    }

    public record AblationEvaluationReport(
            int k,
            int postVectorCount,
            int chunkVectorCount,
            String embeddingModel,
            List<AblationVariantResult> variants) {
    }

    public record AblationVariantResult(
            String id,
            String label,
            String description,
            double averagePrecisionAtK,
            double averageRecallAtK,
            double meanReciprocalRankAtK,
            double meanNdcgAtK,
            double hitRateAtK,
            List<AblationCaseResult> cases) {
    }

    public record AblationCaseResult(
            String id,
            int relevantTotal,
            int retrievedCount,
            double precisionAtK,
            double recallAtK,
            double mrrAtK,
            double ndcgAtK,
            double hitAtK,
            List<AblationRetrievedPost> retrievedPosts) {
    }

    public record AblationRetrievedPost(
            Long postId,
            String title,
            String category,
            double score,
            boolean relevant) {
    }
}
