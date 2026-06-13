package com.jungle_choi.namanmu.service;

import com.jungle_choi.namanmu.domain.post.Post;
import com.jungle_choi.namanmu.domain.post.PostRepository;
import com.jungle_choi.namanmu.domain.post.PostStatus;
import com.jungle_choi.namanmu.domain.post.PostTagRepository;
import com.jungle_choi.namanmu.domain.read.PostReadRepository;
import com.jungle_choi.namanmu.domain.user.User;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AgentRecommendationService {

    private static final int MAX_AGENT_STEPS = 4;
    private static final int READ_HISTORY_LIMIT = 50;
    private static final int CANDIDATE_LIMIT = 80;
    private static final int DEFAULT_RECOMMENDATION_LIMIT = 5;
    private static final int MAX_RECOMMENDATION_LIMIT = 5;
    private static final String SUMMARY_INSTRUCTIONS = """
            You are the Project Alpha missed-posts agent.
            Use only the provided recommendation tool results.
            Write concise Korean text.
            Explain why these unread posts are worth reading for this user.
            Do not invent facts outside the provided titles, categories, tags, and excerpts.
            """;

    private final PostReadRepository postReadRepository;
    private final PostRepository postRepository;
    private final PostTagRepository postTagRepository;
    private final OpenAiTextClient openAiTextClient;

    public AgentRecommendationService(
            PostReadRepository postReadRepository,
            PostRepository postRepository,
            PostTagRepository postTagRepository,
            OpenAiTextClient openAiTextClient) {
        this.postReadRepository = postReadRepository;
        this.postRepository = postRepository;
        this.postTagRepository = postTagRepository;
        this.openAiTextClient = openAiTextClient;
    }

    public AgentRecommendationResult recommendMissedPosts(User user, int limit) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        AgentState state = new AgentState(user, normalizeLimit(limit));
        List<AgentStep> steps = new ArrayList<>();
        int executedStepCount = 0;

        while (!state.done && executedStepCount < MAX_AGENT_STEPS) {
            AgentTool nextTool = chooseNextTool(state);
            steps.add(runTool(nextTool, state));
            executedStepCount++;
        }

        if (!state.done) {
            steps.add(new AgentStep(
                    "stop",
                    "stopped",
                    "Stopped because the agent reached the maximum step count."));
        }

        return new AgentRecommendationResult(
                state.message(),
                state.summary,
                state.recommendations,
                List.copyOf(steps));
    }

    private AgentTool chooseNextTool(AgentState state) {
        if (state.preference == null) {
            return AgentTool.ANALYZE_PROFILE;
        }

        if (state.candidates == null) {
            return AgentTool.FIND_UNREAD_CANDIDATES;
        }

        if (state.recommendations == null) {
            return AgentTool.RANK_RECOMMENDATIONS;
        }

        return AgentTool.SUMMARIZE_RECOMMENDATIONS;
    }

    private AgentStep runTool(AgentTool tool, AgentState state) {
        return switch (tool) {
            case ANALYZE_PROFILE -> analyzeProfile(state);
            case FIND_UNREAD_CANDIDATES -> findUnreadCandidates(state);
            case RANK_RECOMMENDATIONS -> rankRecommendations(state);
            case SUMMARIZE_RECOMMENDATIONS -> summarizeRecommendations(state);
        };
    }

    private AgentStep analyzeProfile(AgentState state) {
        List<Post> readPosts = postReadRepository.findRecentReadPosts(
                state.user.getId(),
                PostStatus.PUBLISHED,
                PageRequest.of(0, READ_HISTORY_LIMIT));
        state.preference = buildPreference(readPosts);

        return new AgentStep(
                AgentTool.ANALYZE_PROFILE.toolName,
                "completed",
                "Read history analyzed: %d posts, top categories=%s, top tags=%s".formatted(
                        readPosts.size(),
                        state.preference.topCategoryNames(),
                        state.preference.topTagNames()));
    }

    private AgentStep findUnreadCandidates(AgentState state) {
        state.candidates = postRepository.findUnreadPublishedPosts(
                state.user.getId(),
                PostStatus.PUBLISHED,
                PageRequest.of(0, CANDIDATE_LIMIT));

        return new AgentStep(
                AgentTool.FIND_UNREAD_CANDIDATES.toolName,
                "completed",
                "Unread candidate posts loaded: %d".formatted(state.candidates.size()));
    }

    private AgentStep rankRecommendations(AgentState state) {
        List<RecommendedPost> recommendations = state.candidates.stream()
                .map((post) -> toScoredRecommendation(post, state.preference, state.candidates))
                .sorted(Comparator.comparingDouble(RecommendedPost::score).reversed()
                        .thenComparing(RecommendedPost::postId))
                .limit(state.limit)
                .toList();
        state.recommendations = recommendations;

        return new AgentStep(
                AgentTool.RANK_RECOMMENDATIONS.toolName,
                "completed",
                "Ranked recommendations: %d selected from %d candidates.".formatted(
                        recommendations.size(),
                        state.candidates.size()));
    }

    private AgentStep summarizeRecommendations(AgentState state) {
        if (state.recommendations.isEmpty()) {
            state.summary = "아직 추천할 만한 안 읽은 글을 찾지 못했습니다.";
            state.done = true;
            return new AgentStep(
                    AgentTool.SUMMARIZE_RECOMMENDATIONS.toolName,
                    "completed",
                    "No recommendations were available, so the agent returned an empty-state message.");
        }

        state.summary = generateSummaryOrFallback(state);
        state.done = true;

        return new AgentStep(
                AgentTool.SUMMARIZE_RECOMMENDATIONS.toolName,
                "completed",
                "Recommendation summary generated.");
    }

    private UserPreference buildPreference(List<Post> readPosts) {
        Map<String, Integer> categoryCounts = new HashMap<>();
        Map<String, Integer> tagCounts = new HashMap<>();

        for (Post post : readPosts) {
            categoryCounts.merge(post.getCategory(), 1, Integer::sum);
            for (String tag : tagsForPost(post)) {
                tagCounts.merge(normalizeKey(tag), 1, Integer::sum);
            }
        }

        return new UserPreference(
                normalizeWeights(categoryCounts),
                normalizeWeights(tagCounts),
                readPosts.size());
    }

    private RecommendedPost toScoredRecommendation(
            Post post,
            UserPreference preference,
            List<Post> candidates) {
        List<String> tags = tagsForPost(post);
        double categoryScore = preference.weightForCategory(post.getCategory());
        double tagScore = tags.stream()
                .map(AgentRecommendationService::normalizeKey)
                .mapToDouble(preference::weightForTag)
                .sum();
        double normalizedTagScore = Math.min(tagScore, 1.0);
        double recencyScore = recencyScore(post, candidates);
        double score = preference.hasHistory()
                ? (categoryScore * 0.45) + (normalizedTagScore * 0.35) + (recencyScore * 0.20)
                : recencyScore;

        return new RecommendedPost(
                post.getId(),
                post.getTitle(),
                post.getCategory(),
                excerpt(post.getContent()),
                tags,
                roundScore(score),
                reason(post, tags, preference, categoryScore, normalizedTagScore));
    }

    private double recencyScore(Post post, List<Post> candidates) {
        int index = candidates.indexOf(post);

        if (index < 0 || candidates.isEmpty()) {
            return 0.0;
        }

        return 1.0 - (index / (double) Math.max(candidates.size(), 1));
    }

    private String reason(
            Post post,
            List<String> tags,
            UserPreference preference,
            double categoryScore,
            double tagScore) {
        if (!preference.hasHistory()) {
            return "아직 읽은 글이 적어서, 최근 올라온 안 읽은 글부터 추천했습니다.";
        }

        List<String> matchedTags = tags.stream()
                .filter((tag) -> preference.weightForTag(normalizeKey(tag)) > 0.0)
                .toList();

        if (!matchedTags.isEmpty()) {
            return "최근 읽은 글의 태그와 겹칩니다: %s".formatted(String.join(", ", matchedTags));
        }

        if (categoryScore > 0.0) {
            return "최근 자주 읽은 %s 카테고리의 안 읽은 글입니다.".formatted(post.getCategory());
        }

        if (tagScore > 0.0) {
            return "관심 태그와 일부 겹치는 안 읽은 글입니다.";
        }

        return "최근 올라온 안 읽은 글 중 읽어볼 만한 후보입니다.";
    }

    private String generateSummaryOrFallback(AgentState state) {
        try {
            return openAiTextClient.generateText(
                    SUMMARY_INSTRUCTIONS,
                    summaryInput(state))
                    .text();
        } catch (RuntimeException exception) {
            return fallbackSummary(state.recommendations);
        }
    }

    private String summaryInput(AgentState state) {
        String recommendations = state.recommendations.stream()
                .map((post) -> """
                        - title: %s
                          category: %s
                          tags: %s
                          reason: %s
                          excerpt: %s
                        """.formatted(
                        post.title(),
                        post.category(),
                        post.tags().isEmpty() ? "None" : String.join(", ", post.tags()),
                        post.reason(),
                        post.excerpt()))
                .collect(Collectors.joining("\n"));

        return """
                User profile:
                - recent read count: %d
                - preferred categories: %s
                - preferred tags: %s

                Candidate recommendations:
                %s
                """.formatted(
                state.preference.readCount(),
                state.preference.topCategoryNames(),
                state.preference.topTagNames(),
                recommendations);
    }

    private static String fallbackSummary(List<RecommendedPost> recommendations) {
        String titles = recommendations.stream()
                .limit(3)
                .map(RecommendedPost::title)
                .collect(Collectors.joining(", "));

        return "읽은 글 기록과 안 읽은 글 후보를 비교해 추천했습니다. 먼저 살펴볼 글은 %s 입니다.".formatted(titles);
    }

    private List<String> tagsForPost(Post post) {
        return postTagRepository.findAllByPostIdOrderByTagNameAsc(post.getId())
                .stream()
                .map((postTag) -> postTag.getTag().getName())
                .toList();
    }

    private static Map<String, Double> normalizeWeights(Map<String, Integer> counts) {
        if (counts.isEmpty()) {
            return Map.of();
        }

        int maxCount = counts.values()
                .stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(1);
        Map<String, Double> weights = new LinkedHashMap<>();
        counts.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry::getKey))
                .forEach((entry) -> weights.put(
                        normalizeKey(entry.getKey()),
                        entry.getValue() / (double) maxCount));

        return weights;
    }

    private static String excerpt(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }

        String normalized = content.replaceAll("\\s+", " ").trim();

        if (normalized.length() <= 180) {
            return normalized;
        }

        return normalized.substring(0, 180) + "...";
    }

    private static String normalizeKey(String value) {
        if (value == null) {
            return "";
        }

        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_RECOMMENDATION_LIMIT;
        }

        return Math.min(limit, MAX_RECOMMENDATION_LIMIT);
    }

    private static double roundScore(double score) {
        return Math.round(score * 10000.0) / 10000.0;
    }

    private enum AgentTool {
        ANALYZE_PROFILE("analyze_profile"),
        FIND_UNREAD_CANDIDATES("find_unread_candidates"),
        RANK_RECOMMENDATIONS("rank_recommendations"),
        SUMMARIZE_RECOMMENDATIONS("summarize_recommendations");

        private final String toolName;

        AgentTool(String toolName) {
            this.toolName = toolName;
        }
    }

    private static final class AgentState {
        private final User user;
        private final int limit;
        private UserPreference preference;
        private List<Post> candidates;
        private List<RecommendedPost> recommendations;
        private String summary = "";
        private boolean done = false;

        private AgentState(User user, int limit) {
            this.user = user;
            this.limit = limit;
        }

        private String message() {
            if (recommendations == null || recommendations.isEmpty()) {
                return "No unread recommendations found.";
            }

            return "Recommended %d missed posts.".formatted(recommendations.size());
        }
    }

    private record UserPreference(
            Map<String, Double> categoryWeights,
            Map<String, Double> tagWeights,
            int readCount) {

        private boolean hasHistory() {
            return readCount > 0;
        }

        private double weightForCategory(String category) {
            return categoryWeights.getOrDefault(normalizeKey(category), 0.0);
        }

        private double weightForTag(String tag) {
            return tagWeights.getOrDefault(normalizeKey(tag), 0.0);
        }

        private Set<String> topCategoryNames() {
            return topNames(categoryWeights);
        }

        private Set<String> topTagNames() {
            return topNames(tagWeights);
        }

        private static Set<String> topNames(Map<String, Double> weights) {
            return weights.entrySet()
                    .stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed()
                            .thenComparing(Map.Entry::getKey))
                    .limit(5)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toCollection(HashSet::new));
        }
    }

    public record AgentRecommendationResult(
            String message,
            String summary,
            List<RecommendedPost> recommendations,
            List<AgentStep> steps) {
    }

    public record RecommendedPost(
            Long postId,
            String title,
            String category,
            String excerpt,
            List<String> tags,
            double score,
            String reason) {
    }

    public record AgentStep(
            String tool,
            String status,
            String observation) {
    }
}
