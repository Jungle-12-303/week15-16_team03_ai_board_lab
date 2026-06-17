package com.jungle_choi.namanmu.service.rag;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jungle_choi.namanmu.domain.embedding.PostEmbedding;
import com.jungle_choi.namanmu.domain.embedding.PostEmbeddingChunk;
import com.jungle_choi.namanmu.domain.embedding.PostEmbeddingChunkRepository;
import com.jungle_choi.namanmu.domain.embedding.PostEmbeddingRepository;
import com.jungle_choi.namanmu.domain.post.Post;
import com.jungle_choi.namanmu.domain.post.PostRepository;
import com.jungle_choi.namanmu.service.rag.OpenAiEmbeddingClient.EmbeddingResult;
import com.jungle_choi.namanmu.service.rag.PostEmbeddingTextBuilder.ChunkSourceText;
import com.jungle_choi.namanmu.service.rag.QdrantVectorStoreClient.ChunkVectorPoint;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostEmbeddingService {

    private static final String HASH_ALGORITHM = "SHA-256";

    private final PostEmbeddingRepository postEmbeddingRepository;
    private final PostEmbeddingChunkRepository postEmbeddingChunkRepository;
    private final PostRepository postRepository;
    private final ObjectMapper objectMapper;
    private final QdrantVectorStoreClient qdrantVectorStoreClient;

    public PostEmbeddingService(
            PostEmbeddingRepository postEmbeddingRepository,
            PostEmbeddingChunkRepository postEmbeddingChunkRepository,
            PostRepository postRepository,
            ObjectMapper objectMapper,
            QdrantVectorStoreClient qdrantVectorStoreClient) {
        this.postEmbeddingRepository = postEmbeddingRepository;
        this.postEmbeddingChunkRepository = postEmbeddingChunkRepository;
        this.postRepository = postRepository;
        this.objectMapper = objectMapper;
        this.qdrantVectorStoreClient = qdrantVectorStoreClient;
    }

    @Transactional
    public void saveOrReplace(Long postId, String sourceText, EmbeddingResult embeddingResult) {
        Post post = postRepository.getReferenceById(postId);
        String embeddingJson = toJson(embeddingResult);
        String sourceHash = hash(sourceText);

        postEmbeddingRepository.findByPost_Id(postId)
                .ifPresentOrElse(
                        (postEmbedding) -> postEmbedding.replace(
                                embeddingResult.model(),
                                embeddingResult.dimensions(),
                                embeddingJson,
                                sourceHash),
                        () -> postEmbeddingRepository.save(PostEmbedding.create(
                                post,
                                embeddingResult.model(),
                                embeddingResult.dimensions(),
                                embeddingJson,
                                sourceHash)));
        qdrantVectorStoreClient.upsertPost(
                postId,
                embeddingResult.model(),
                embeddingResult.embedding());
    }

    @Transactional
    public void saveOrReplaceChunks(Long postId, List<ChunkEmbeddingInput> chunkEmbeddings) {
        Post post = postRepository.getReferenceById(postId);
        postEmbeddingChunkRepository.deleteAllByPost_Id(postId);
        // Existing chunks reuse the same post_id/chunk_index unique keys, so delete SQL must run first.
        postEmbeddingChunkRepository.flush();

        List<PostEmbeddingChunk> chunks = chunkEmbeddings.stream()
                .map((chunkEmbedding) -> PostEmbeddingChunk.create(
                        post,
                        chunkEmbedding.chunkIndex(),
                        chunkEmbedding.chunkText(),
                        chunkEmbedding.embeddingResult().model(),
                        chunkEmbedding.embeddingResult().dimensions(),
                        toJson(chunkEmbedding.embeddingResult()),
                        hash(chunkEmbedding.sourceText())))
                .toList();

        postEmbeddingChunkRepository.saveAll(chunks);
        qdrantVectorStoreClient.replacePostChunks(
                postId,
                chunkEmbeddings.stream()
                        .map((chunkEmbedding) -> new ChunkVectorPoint(
                                chunkEmbedding.chunkIndex(),
                                chunkEmbedding.embeddingResult().model(),
                                chunkEmbedding.embeddingResult().embedding()))
                        .toList());
    }

    public boolean chunksAreUpToDate(
            Long postId,
            String embeddingModel,
            List<ChunkSourceText> chunkSources) {
        long savedChunkCount = postEmbeddingChunkRepository.countByPost_IdAndEmbeddingModel(
                postId,
                embeddingModel);

        if (savedChunkCount != chunkSources.size()) {
            return false;
        }

        return chunkSources.stream()
                .allMatch((chunkSource) ->
                        postEmbeddingChunkRepository.existsByPost_IdAndChunkIndexAndEmbeddingModelAndSourceHash(
                                postId,
                                chunkSource.chunkIndex(),
                                embeddingModel,
                                sourceHash(chunkSource.sourceText())));
    }

    public String sourceHash(String sourceText) {
        return hash(sourceText);
    }

    private String toJson(EmbeddingResult embeddingResult) {
        try {
            return objectMapper.writeValueAsString(embeddingResult.embedding());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Embedding vector could not be serialized.", exception);
        }
    }

    private static String hash(String sourceText) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] digest = messageDigest.digest(normalize(sourceText).getBytes(StandardCharsets.UTF_8));

            return toHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(HASH_ALGORITHM + " is not available.", exception);
        }
    }

    private static String normalize(String sourceText) {
        if (sourceText == null) {
            return "";
        }

        return sourceText.trim();
    }

    private static String toHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);

        for (byte value : bytes) {
            hex.append(String.format("%02x", value));
        }

        return hex.toString();
    }

    public record ChunkEmbeddingInput(
            int chunkIndex,
            String chunkText,
            String sourceText,
            EmbeddingResult embeddingResult) {
    }
}
