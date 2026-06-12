package com.jungle_choi.namanmu.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jungle_choi.namanmu.config.OpenAiProperties;
import com.jungle_choi.namanmu.domain.embedding.PostEmbedding;
import com.jungle_choi.namanmu.domain.embedding.PostEmbeddingRepository;
import com.jungle_choi.namanmu.domain.post.Post;
import com.jungle_choi.namanmu.domain.post.PostTag;
import com.jungle_choi.namanmu.domain.post.PostTagRepository;
import com.jungle_choi.namanmu.domain.tag.Tag;
import com.jungle_choi.namanmu.domain.user.User;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

class SimilarPostSearchServiceTest {

    private static final String EMBEDDING_MODEL = "text-embedding-3-small";

    private final PostEmbeddingRepository postEmbeddingRepository =
            Mockito.mock(PostEmbeddingRepository.class);
    private final PostTagRepository postTagRepository =
            Mockito.mock(PostTagRepository.class);
    private final SimilarPostSearchService similarPostSearchService =
            new SimilarPostSearchService(
                    postEmbeddingRepository,
                    postTagRepository,
                    new OpenAiProperties(
                            "test-key",
                            "https://api.openai.com/v1",
                            "gpt-4.1-mini",
                            EMBEDDING_MODEL,
                            30),
                    new ObjectMapper());

    @Test
    void searchSimilarPostsReturnsHighScoreFirst() {
        when(postEmbeddingRepository.findAllByEmbeddingModel(EMBEDDING_MODEL))
                .thenReturn(List.of(
                        embedding(1L, "same direction", "[1.0,0.0]"),
                        embedding(2L, "diagonal direction", "[0.5,0.5]"),
                        embedding(3L, "different direction", "[0.0,1.0]")));

        List<SimilarPostSearchService.SimilarPostResult> results =
                similarPostSearchService.searchSimilarPosts(List.of(1.0, 0.0), null, 2);

        assertThat(results)
                .extracting(SimilarPostSearchService.SimilarPostResult::postId)
                .containsExactly(1L, 2L);
    }

    @Test
    void searchSimilarPostsExcludesCurrentPost() {
        when(postEmbeddingRepository.findAllByEmbeddingModel(EMBEDDING_MODEL))
                .thenReturn(List.of(
                        embedding(1L, "current post", "[1.0,0.0]"),
                        embedding(2L, "other post", "[0.5,0.5]")));

        List<SimilarPostSearchService.SimilarPostResult> results =
                similarPostSearchService.searchSimilarPosts(List.of(1.0, 0.0), 1L, 5);

        assertThat(results)
                .extracting(SimilarPostSearchService.SimilarPostResult::postId)
                .containsExactly(2L);
    }

    @Test
    void searchSimilarPostsBoostsDirectKeywordMatches() {
        when(postEmbeddingRepository.findAllByEmbeddingModel(EMBEDDING_MODEL))
                .thenReturn(List.of(
                        embedding(1L, "쿠키런 런칭 회고", "서비스 회고 내용", "[1.0,0.0]"),
                        embedding(2L, "GitHub Actions 자동화", "GitHub 워크플로와 CI 정리", "[0.0,1.0]")));

        List<SimilarPostSearchService.SimilarPostResult> results =
                similarPostSearchService.searchSimilarPosts(
                        List.of(1.0, 0.0),
                        null,
                        2,
                        "All",
                        "깃허브",
                        "깃허브 사용법을 정리하고 싶다.",
                        List.of());

        assertThat(results)
                .extracting(SimilarPostSearchService.SimilarPostResult::postId)
                .containsExactly(2L);
    }

    @Test
    void searchSimilarPostsFiltersByCategory() {
        when(postEmbeddingRepository.findAllByEmbeddingModel(EMBEDDING_MODEL))
                .thenReturn(List.of(
                        embeddingWithCategory(1L, "learning post", "Learning", "[1.0,0.0]"),
                        embedding(2L, "프로젝트 회고", "Project", "프로젝트 진행 내용을 정리한다.", "[0.5,0.5]")));

        List<SimilarPostSearchService.SimilarPostResult> results =
                similarPostSearchService.searchSimilarPosts(
                        List.of(1.0, 0.0),
                        null,
                        5,
                        "Project",
                        "프로젝트 회고",
                        "프로젝트 진행 내용을 정리한다.",
                        List.of());

        assertThat(results)
                .extracting(SimilarPostSearchService.SimilarPostResult::postId)
                .containsExactly(2L);
    }

    @Test
    void searchSimilarPostsFiltersByAnyMatchingTag() {
        PostEmbedding springPost = embeddingWithCategory(1L, "Spring Boot auth", "Development", "[1.0,0.0]");
        PostEmbedding reactPost = embeddingWithCategory(2L, "React state", "Development", "[0.9,0.1]");
        when(postEmbeddingRepository.findAllByEmbeddingModel(EMBEDDING_MODEL))
                .thenReturn(List.of(springPost, reactPost));
        when(postTagRepository.findAllByPostIdOrderByTagNameAsc(1L))
                .thenReturn(List.of(postTag(springPost.getPost(), "Spring")));
        when(postTagRepository.findAllByPostIdOrderByTagNameAsc(2L))
                .thenReturn(List.of(postTag(reactPost.getPost(), "React")));

        List<SimilarPostSearchService.SimilarPostResult> results =
                similarPostSearchService.searchSimilarPosts(
                        List.of(1.0, 0.0),
                        null,
                        5,
                        "Development",
                        "Spring JWT",
                        "Spring Security와 JWT 로그인을 정리한다.",
                        List.of("JWT", "Spring"));

        assertThat(results)
                .extracting(SimilarPostSearchService.SimilarPostResult::postId)
                .containsExactly(1L);
    }

    @Test
    void cosineSimilarityReturnsZeroForZeroVector() {
        double score = SimilarPostSearchService.cosineSimilarity(
                List.of(0.0, 0.0),
                List.of(1.0, 0.0));

        assertThat(score).isZero();
    }

    private static PostEmbedding embedding(Long postId, String title, String embeddingJson) {
        return embedding(postId, title, "Learning", embeddingJson);
    }

    private static PostEmbedding embedding(Long postId, String title, String content, String embeddingJson) {
        return embedding(postId, title, "Learning", content, embeddingJson);
    }

    private static PostEmbedding embeddingWithCategory(
            Long postId,
            String title,
            String category,
            String embeddingJson) {
        return embedding(postId, title, category, "content", embeddingJson);
    }

    private static PostEmbedding embedding(
            Long postId,
            String title,
            String category,
            String content,
            String embeddingJson) {
        User author = User.createLocalUser("cedis");
        Post post = Post.create(author, category, title, content);
        ReflectionTestUtils.setField(post, "id", postId);

        return PostEmbedding.create(
                post,
                EMBEDDING_MODEL,
                2,
                embeddingJson,
                "hash-" + postId);
    }

    private static PostTag postTag(Post post, String tagName) {
        return PostTag.create(post, Tag.create(tagName));
    }
}
