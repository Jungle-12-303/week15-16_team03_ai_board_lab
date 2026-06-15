package com.jungle_choi.namanmu.service.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.jungle_choi.namanmu.domain.post.Post;
import com.jungle_choi.namanmu.domain.post.PostRepository;
import com.jungle_choi.namanmu.domain.post.PostStatus;
import com.jungle_choi.namanmu.domain.post.PostTag;
import com.jungle_choi.namanmu.domain.post.PostTagRepository;
import com.jungle_choi.namanmu.domain.read.PostReadRepository;
import com.jungle_choi.namanmu.domain.tag.Tag;
import com.jungle_choi.namanmu.domain.user.User;
import com.jungle_choi.namanmu.service.rag.OpenAiTextClient;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

class AgentRecommendationServiceTest {

    private final PostReadRepository postReadRepository =
            Mockito.mock(PostReadRepository.class);
    private final PostRepository postRepository =
            Mockito.mock(PostRepository.class);
    private final PostTagRepository postTagRepository =
            Mockito.mock(PostTagRepository.class);
    private final OpenAiTextClient openAiTextClient =
            Mockito.mock(OpenAiTextClient.class);
    private final AgentRecommendationService agentRecommendationService =
            new AgentRecommendationService(
                    postReadRepository,
                    postRepository,
                    postTagRepository,
                    openAiTextClient);

    @Test
    void recommendMissedPostsRunsAgentToolsAndRanksUnreadPosts() {
        User user = user(1L, "cedis");
        Post readPost = post(10L, "Learning", "React state notes");
        Post matchingUnreadPost = post(20L, "Learning", "React hooks guide");
        Post otherUnreadPost = post(21L, "Daily", "Small daily log");

        when(postReadRepository.findRecentReadPosts(
                eq(user.getId()),
                eq(PostStatus.PUBLISHED),
                any(Pageable.class)))
                .thenReturn(List.of(readPost));
        when(postRepository.findUnreadPublishedPosts(
                eq(user.getId()),
                eq(PostStatus.PUBLISHED),
                any(Pageable.class)))
                .thenReturn(List.of(otherUnreadPost, matchingUnreadPost));
        when(postTagRepository.findAllByPostIdOrderByTagNameAsc(readPost.getId()))
                .thenReturn(List.of(postTag(readPost, "React")));
        when(postTagRepository.findAllByPostIdOrderByTagNameAsc(matchingUnreadPost.getId()))
                .thenReturn(List.of(postTag(matchingUnreadPost, "React")));
        when(postTagRepository.findAllByPostIdOrderByTagNameAsc(otherUnreadPost.getId()))
                .thenReturn(List.of(postTag(otherUnreadPost, "Daily")));
        when(openAiTextClient.generateText(any(String.class), any(String.class)))
                .thenReturn(new OpenAiTextClient.TextGenerationResult("React 글을 먼저 확인하면 좋습니다."));

        AgentRecommendationService.AgentRecommendationResult result =
                agentRecommendationService.recommendMissedPosts(user, 5);

        assertThat(result.recommendations())
                .extracting(AgentRecommendationService.RecommendedPost::postId)
                .startsWith(matchingUnreadPost.getId());
        AgentRecommendationService.RecommendedPost firstRecommendation =
                result.recommendations().get(0);
        assertThat(firstRecommendation.score()).isEqualTo(0.9);
        assertThat(firstRecommendation.scoreBreakdown().categoryContribution()).isEqualTo(0.45);
        assertThat(firstRecommendation.scoreBreakdown().tagContribution()).isEqualTo(0.35);
        assertThat(firstRecommendation.scoreBreakdown().recencyContribution()).isEqualTo(0.1);
        assertThat(firstRecommendation.scoreBreakdown().matchedTags()).containsExactly("React");
        assertThat(result.summary()).isEqualTo("React 글을 먼저 확인하면 좋습니다.");
        assertThat(result.steps())
                .extracting(AgentRecommendationService.AgentStep::tool)
                .containsExactly(
                        "analyze_profile",
                        "find_unread_candidates",
                        "rank_recommendations",
                        "summarize_recommendations");
    }

    @Test
    void recommendMissedPostsWeightsRecentReadHistoryMoreThanOlderReads() {
        User user = user(1L, "cedis");
        Post recentReadPost = post(10L, "Learning", "React state notes");
        Post olderReadPost = post(11L, "Daily", "Daily hardware memo");
        Post dailyUnreadPost = post(20L, "Daily", "Daily hardware follow up");
        Post reactUnreadPost = post(21L, "Learning", "React hooks guide");

        when(postReadRepository.findRecentReadPosts(
                eq(user.getId()),
                eq(PostStatus.PUBLISHED),
                any(Pageable.class)))
                .thenReturn(List.of(recentReadPost, olderReadPost));
        when(postRepository.findUnreadPublishedPosts(
                eq(user.getId()),
                eq(PostStatus.PUBLISHED),
                any(Pageable.class)))
                .thenReturn(List.of(dailyUnreadPost, reactUnreadPost));
        when(postTagRepository.findAllByPostIdOrderByTagNameAsc(recentReadPost.getId()))
                .thenReturn(List.of(postTag(recentReadPost, "React")));
        when(postTagRepository.findAllByPostIdOrderByTagNameAsc(olderReadPost.getId()))
                .thenReturn(List.of(postTag(olderReadPost, "Hardware")));
        when(postTagRepository.findAllByPostIdOrderByTagNameAsc(dailyUnreadPost.getId()))
                .thenReturn(List.of(postTag(dailyUnreadPost, "Hardware")));
        when(postTagRepository.findAllByPostIdOrderByTagNameAsc(reactUnreadPost.getId()))
                .thenReturn(List.of(postTag(reactUnreadPost, "React")));
        when(openAiTextClient.generateText(any(String.class), any(String.class)))
                .thenReturn(new OpenAiTextClient.TextGenerationResult("최근 React 관심사를 기준으로 추천했습니다."));

        AgentRecommendationService.AgentRecommendationResult result =
                agentRecommendationService.recommendMissedPosts(user, 5);

        assertThat(result.recommendations())
                .extracting(AgentRecommendationService.RecommendedPost::postId)
                .startsWith(reactUnreadPost.getId());
        assertThat(result.recommendations().get(0).scoreBreakdown().matchedTags())
                .containsExactly("React");
    }

    private static User user(Long userId, String name) {
        User user = User.createLocalUser(name);
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }

    private static Post post(Long postId, String category, String title) {
        Post post = Post.create(user(999L + postId, "author-" + postId), category, title, title + " content");
        ReflectionTestUtils.setField(post, "id", postId);
        return post;
    }

    private static PostTag postTag(Post post, String tagName) {
        return PostTag.create(post, Tag.create(tagName));
    }
}
