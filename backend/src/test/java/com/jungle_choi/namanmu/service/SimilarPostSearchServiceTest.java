package com.jungle_choi.namanmu.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jungle_choi.namanmu.config.OpenAiProperties;
import com.jungle_choi.namanmu.domain.embedding.PostEmbedding;
import com.jungle_choi.namanmu.domain.embedding.PostEmbeddingRepository;
import com.jungle_choi.namanmu.domain.post.Post;
import com.jungle_choi.namanmu.domain.user.User;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

class SimilarPostSearchServiceTest {

    private static final String EMBEDDING_MODEL = "text-embedding-3-small";

    private final PostEmbeddingRepository postEmbeddingRepository =
            Mockito.mock(PostEmbeddingRepository.class);
    private final SimilarPostSearchService similarPostSearchService =
            new SimilarPostSearchService(
                    postEmbeddingRepository,
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
    void cosineSimilarityReturnsZeroForZeroVector() {
        double score = SimilarPostSearchService.cosineSimilarity(
                List.of(0.0, 0.0),
                List.of(1.0, 0.0));

        assertThat(score).isZero();
    }

    private static PostEmbedding embedding(Long postId, String title, String embeddingJson) {
        User author = User.createLocalUser("cedis");
        Post post = Post.create(author, "Learning", title, "content");
        ReflectionTestUtils.setField(post, "id", postId);

        return PostEmbedding.create(
                post,
                EMBEDDING_MODEL,
                2,
                embeddingJson,
                "hash-" + postId);
    }
}
