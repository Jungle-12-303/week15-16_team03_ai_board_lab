package com.jungle_choi.namanmu.service.rag;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jungle_choi.namanmu.domain.embedding.PostEmbeddingChunkRepository;
import com.jungle_choi.namanmu.domain.embedding.PostEmbeddingRepository;
import com.jungle_choi.namanmu.domain.post.Post;
import com.jungle_choi.namanmu.domain.post.PostRepository;
import com.jungle_choi.namanmu.domain.user.User;
import com.jungle_choi.namanmu.service.rag.OpenAiEmbeddingClient.EmbeddingResult;
import com.jungle_choi.namanmu.service.rag.PostEmbeddingService.ChunkEmbeddingInput;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

class PostEmbeddingServiceTest {

    private static final String EMBEDDING_MODEL = "text-embedding-3-small";

    private final PostEmbeddingRepository postEmbeddingRepository =
            Mockito.mock(PostEmbeddingRepository.class);
    private final PostEmbeddingChunkRepository postEmbeddingChunkRepository =
            Mockito.mock(PostEmbeddingChunkRepository.class);
    private final PostRepository postRepository =
            Mockito.mock(PostRepository.class);
    private final QdrantVectorStoreClient qdrantVectorStoreClient =
            Mockito.mock(QdrantVectorStoreClient.class);
    private final PostEmbeddingService postEmbeddingService =
            new PostEmbeddingService(
                    postEmbeddingRepository,
                    postEmbeddingChunkRepository,
                    postRepository,
                    new ObjectMapper(),
                    qdrantVectorStoreClient);

    @Test
    void saveOrReplaceChunksFlushesDeletedChunksBeforeSavingReplacementChunks() {
        Long postId = 1L;
        when(postRepository.getReferenceById(postId)).thenReturn(post(postId));

        postEmbeddingService.saveOrReplaceChunks(
                postId,
                List.of(new ChunkEmbeddingInput(
                        0,
                        "new chunk text",
                        "new chunk source",
                        new EmbeddingResult(
                                EMBEDDING_MODEL,
                                List.of(0.1, 0.2),
                                3))));

        InOrder inOrder = inOrder(postEmbeddingChunkRepository, qdrantVectorStoreClient);
        inOrder.verify(postEmbeddingChunkRepository).deleteAllByPost_Id(postId);
        inOrder.verify(postEmbeddingChunkRepository).flush();
        inOrder.verify(postEmbeddingChunkRepository).saveAll(anyList());
        inOrder.verify(qdrantVectorStoreClient).replacePostChunks(eq(postId), anyList());
    }

    private static Post post(Long postId) {
        User author = User.createLocalUser("cedis");
        ReflectionTestUtils.setField(author, "id", 1L);
        Post post = Post.create(author, "Learning", "title", "content");
        ReflectionTestUtils.setField(post, "id", postId);

        return post;
    }
}
