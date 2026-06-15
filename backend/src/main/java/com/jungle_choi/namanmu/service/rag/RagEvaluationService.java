package com.jungle_choi.namanmu.service.rag;

import com.jungle_choi.namanmu.domain.post.Post;
import com.jungle_choi.namanmu.domain.post.PostRepository;
import com.jungle_choi.namanmu.domain.post.PostStatus;
import com.jungle_choi.namanmu.domain.post.PostTagRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RagEvaluationService {

    private static final int DEFAULT_EVALUATION_LIMIT = 5;
    private static final int MAX_EVALUATION_LIMIT = 10;

    private final PostEmbeddingTextBuilder postEmbeddingTextBuilder;
    private final OpenAiEmbeddingClient openAiEmbeddingClient;
    private final SimilarPostSearchService similarPostSearchService;
    private final PostRepository postRepository;
    private final PostTagRepository postTagRepository;

    public RagEvaluationService(
            PostEmbeddingTextBuilder postEmbeddingTextBuilder,
            OpenAiEmbeddingClient openAiEmbeddingClient,
            SimilarPostSearchService similarPostSearchService,
            PostRepository postRepository,
            PostTagRepository postTagRepository) {
        this.postEmbeddingTextBuilder = postEmbeddingTextBuilder;
        this.openAiEmbeddingClient = openAiEmbeddingClient;
        this.similarPostSearchService = similarPostSearchService;
        this.postRepository = postRepository;
        this.postTagRepository = postTagRepository;
    }

    @Transactional(readOnly = true)
    public RagEvaluationReport evaluateRetrieval(int requestedLimit) {
        int limit = normalizeLimit(requestedLimit);
        List<Post> publishedPosts = postRepository.findAll()
                .stream()
                .filter((post) -> post.getStatus() == PostStatus.PUBLISHED)
                .toList();
        List<RagEvaluationCaseResult> caseResults = new ArrayList<>();

        for (RagEvaluationCase evaluationCase : evaluationCases()) {
            caseResults.add(evaluateCase(evaluationCase, publishedPosts, limit));
        }

        return new RagEvaluationReport(
                limit,
                average(caseResults.stream()
                        .mapToDouble(RagEvaluationCaseResult::precisionAtK)
                        .toArray()),
                average(caseResults.stream()
                        .mapToDouble(RagEvaluationCaseResult::recallAtK)
                        .toArray()),
                average(caseResults.stream()
                        .mapToDouble(RagEvaluationCaseResult::mrrAtK)
                        .toArray()),
                average(caseResults.stream()
                        .mapToDouble(RagEvaluationCaseResult::ndcgAtK)
                        .toArray()),
                average(caseResults.stream()
                        .mapToDouble(RagEvaluationCaseResult::hitAtK)
                        .toArray()),
                caseResults);
    }

    private RagEvaluationCaseResult evaluateCase(
            RagEvaluationCase evaluationCase,
            List<Post> publishedPosts,
            int limit) {
        String queryText = postEmbeddingTextBuilder.buildQuery(
                evaluationCase.category(),
                evaluationCase.title(),
                evaluationCase.content(),
                evaluationCase.tags());
        OpenAiEmbeddingClient.EmbeddingResult embeddingResult =
                openAiEmbeddingClient.createEmbedding(queryText);
        List<SimilarPostSearchService.SimilarPostResult> results =
                similarPostSearchService.searchSimilarPosts(
                        embeddingResult.embedding(),
                        null,
                        limit,
                        evaluationCase.category(),
                        evaluationCase.title(),
                        evaluationCase.content(),
                        evaluationCase.tags());
        int relevantTotal = (int) publishedPosts.stream()
                .filter((post) -> isRelevant(evaluationCase, post))
                .count();
        List<RagEvaluationRetrievedPost> retrievedPosts = new ArrayList<>();
        int relevantRetrievedCount = 0;
        int firstRelevantRank = 0;
        double dcg = 0.0;

        for (int index = 0; index < results.size(); index++) {
            SimilarPostSearchService.SimilarPostResult result = results.get(index);
            boolean relevant = isRelevant(evaluationCase, result);
            if (relevant) {
                relevantRetrievedCount++;
                if (firstRelevantRank == 0) {
                    firstRelevantRank = index + 1;
                }
                dcg += discountedGain(index + 1);
            }

            retrievedPosts.add(new RagEvaluationRetrievedPost(
                    result.postId(),
                    result.title(),
                    result.category(),
                    result.score(),
                    relevant,
                    result.scoreBreakdown()));
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

        return new RagEvaluationCaseResult(
                evaluationCase.id(),
                evaluationCase.title(),
                evaluationCase.content(),
                evaluationCase.relevanceRule(),
                relevantTotal,
                retrievedPosts.size(),
                precisionAtK,
                recallAtK,
                mrrAtK,
                ndcgAtK,
                hitAtK,
                retrievedPosts);
    }

    private boolean isRelevant(
            RagEvaluationCase evaluationCase,
            SimilarPostSearchService.SimilarPostResult result) {
        String text = normalize("%s %s %s".formatted(
                result.title(),
                result.category(),
                result.content()));

        return isRelevant(evaluationCase, text);
    }

    private boolean isRelevant(RagEvaluationCase evaluationCase, Post post) {
        String tags = String.join(" ", postTagRepository.findAllByPostIdOrderByTagNameAsc(post.getId())
                .stream()
                .map((postTag) -> postTag.getTag().getName())
                .toList());
        String text = normalize("%s %s %s %s".formatted(
                post.getTitle(),
                post.getCategory(),
                post.getContent(),
                tags));

        return isRelevant(evaluationCase, text);
    }

    private static boolean isRelevant(RagEvaluationCase evaluationCase, String text) {
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

    private static boolean containsAllSubjectTerms(Set<String> subjectTerms, String expectedText) {
        Set<String> expectedTerms = extractSubjectTerms(expectedText);
        if (expectedTerms.isEmpty()) {
            return false;
        }

        return subjectTerms.containsAll(expectedTerms);
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

    private static String normalize(String text) {
        if (text == null) {
            return "";
        }

        return text.toLowerCase(Locale.ROOT)
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
                .replace("그래픽 카드", " gpu ")
                .replace("그래픽카드", " gpu ")
                .replace("팬 소음", " fan noise ")
                .replace("소음", " noise ")
                .replace("언더볼팅", " undervolt ")
                .replace("발열", " temperature ")
                .replace("온도", " temperature ")
                .replace("날씨", " weather ")
                .replace("기상", " weather ")
                .replace("기온", " temperature ")
                .replace("브리핑", " briefing ")
                .replace("리액트", " react ")
                .replace("유즈 스테이트", " usestate ")
                .replace("유즈스테이트", " usestate ")
                .replace("상태 관리", " state ")
                .replace("상태관리", " state ")
                .replace("상태", " state ")
                .replace("훅", " hook ")
                .replace("예보", " forecast ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static Set<String> extractSubjectTerms(String text) {
        String normalizedText = normalize(text);
        Set<String> subjectTerms = new LinkedHashSet<>();

        Arrays.stream(normalizedText.split("[^\\p{L}\\p{N}]+"))
                .map(RagEvaluationService::normalizeToken)
                .filter(RagEvaluationService::isSubjectTerm)
                .forEach(subjectTerms::add);

        return subjectTerms;
    }

    private static String normalizeToken(String token) {
        String particleStrippedToken = stripKoreanParticle(token);

        if (particleStrippedToken.startsWith("업그레이드")) {
            return "upgrade";
        }

        if (particleStrippedToken.startsWith("언더볼팅")) {
            return "undervolt";
        }

        if (particleStrippedToken.startsWith("워크플로")) {
            return "workflow";
        }

        if (particleStrippedToken.startsWith("배포")) {
            return "deploy";
        }

        if (particleStrippedToken.startsWith("소음")) {
            return "noise";
        }

        if (particleStrippedToken.startsWith("하드웨어")) {
            return "hardware";
        }

        return particleStrippedToken;
    }

    private static String stripKoreanParticle(String token) {
        if (token == null) {
            return "";
        }

        for (String suffix : List.of("으로", "에서", "에게", "부터", "까지", "처럼", "보다")) {
            if (token.endsWith(suffix) && token.length() > suffix.length() + 1) {
                return token.substring(0, token.length() - suffix.length());
            }
        }

        for (String suffix : List.of("은", "는", "이", "가", "을", "를", "에", "와", "과", "로", "도", "만", "의")) {
            if (token.endsWith(suffix) && token.length() > suffix.length() + 1) {
                return token.substring(0, token.length() - suffix.length());
            }
        }

        return token;
    }

    private static boolean isSubjectTerm(String token) {
        if (token == null || token.isBlank() || token.length() < 2) {
            return false;
        }

        return !Set.of(
                "this",
                "that",
                "with",
                "from",
                "about",
                "daily",
                "learning",
                "project",
                "development",
                "review",
                "briefing",
                "current",
                "하고",
                "싶다",
                "정리",
                "정리하고",
                "사용",
                "사용법",
                "내용",
                "관련",
                "게시글",
                "작성",
                "찾고",
                "오늘",
                "그냥",
                "지역",
                "짧은",
                "하려고",
                "합니다",
                "현재",
                "다음",
                "위해",
                "위해서",
                "대한",
                "대해",
                "있는",
                "없는",
                "있고",
                "중입니다",
                "했습니다",
                "고민",
                "기록",
                "메모",
                "확인",
                "간단히",
                "짧게",
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

    private record RagEvaluationCase(
            String id,
            String category,
            String title,
            String content,
            List<String> tags,
            List<String> requiredTerms,
            int minimumAnyTermMatches,
            List<String> anyTerms) {

        String relevanceRule() {
            return "required=%s, minimumAnyTermMatches=%d, any=%s".formatted(
                    requiredTerms,
                    minimumAnyTermMatches,
                    anyTerms);
        }
    }

    public record RagEvaluationReport(
            int k,
            double averagePrecisionAtK,
            double averageRecallAtK,
            double meanReciprocalRankAtK,
            double meanNdcgAtK,
            double hitRateAtK,
            List<RagEvaluationCaseResult> cases) {
    }

    public record RagEvaluationCaseResult(
            String id,
            String title,
            String content,
            String relevanceRule,
            int relevantTotal,
            int retrievedCount,
            double precisionAtK,
            double recallAtK,
            double mrrAtK,
            double ndcgAtK,
            double hitAtK,
            List<RagEvaluationRetrievedPost> retrievedPosts) {
    }

    public record RagEvaluationRetrievedPost(
            Long postId,
            String title,
            String category,
            double score,
            boolean relevant,
            SimilarPostSearchService.SearchScoreBreakdown scoreBreakdown) {
    }
}
