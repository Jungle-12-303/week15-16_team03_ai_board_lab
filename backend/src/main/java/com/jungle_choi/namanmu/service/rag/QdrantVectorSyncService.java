package com.jungle_choi.namanmu.service.rag;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jungle_choi.namanmu.config.OpenAiProperties;
import com.jungle_choi.namanmu.domain.embedding.PostEmbedding;
import com.jungle_choi.namanmu.domain.embedding.PostEmbeddingChunk;
import com.jungle_choi.namanmu.domain.embedding.PostEmbeddingChunkRepository;
import com.jungle_choi.namanmu.domain.embedding.PostEmbeddingRepository;
import com.jungle_choi.namanmu.service.rag.QdrantVectorStoreClient.ChunkVectorPoint;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QdrantVectorSyncService {

    private static final int MAX_SYNC_POSTS = 5000;
    private static final TypeReference<List<Double>> EMBEDDING_VECTOR_TYPE = new TypeReference<>() {
    };

    private final PostEmbeddingRepository postEmbeddingRepository;
    private final PostEmbeddingChunkRepository postEmbeddingChunkRepository;
    private final OpenAiProperties openAiProperties;
    private final ObjectMapper objectMapper;
    private final QdrantVectorStoreClient qdrantVectorStoreClient;

    public QdrantVectorSyncService(
            PostEmbeddingRepository postEmbeddingRepository,
            PostEmbeddingChunkRepository postEmbeddingChunkRepository,
            OpenAiProperties openAiProperties,
            ObjectMapper objectMapper,
            QdrantVectorStoreClient qdrantVectorStoreClient) {
        this.postEmbeddingRepository = postEmbeddingRepository;
        this.postEmbeddingChunkRepository = postEmbeddingChunkRepository;
        this.openAiProperties = openAiProperties;
        this.objectMapper = objectMapper;
        this.qdrantVectorStoreClient = qdrantVectorStoreClient;
    }

    @Transactional(readOnly = true)
    public SyncResult syncExistingEmbeddings(int requestedLimit) {
        int limit = normalizeLimit(requestedLimit);
        String embeddingModel = openAiProperties.embeddingModel();
        if (!qdrantVectorStoreClient.isEnabled()) {
            return new SyncResult(
                    embeddingModel,
                    false,
                    0,
                    0,
                    0);
        }

        List<PostEmbedding> postEmbeddings = postEmbeddingRepository.findAllByEmbeddingModel(embeddingModel)
                .stream()
                .limit(limit)
                .toList();

        postEmbeddings.forEach((postEmbedding) ->
                qdrantVectorStoreClient.upsertPost(
                        postEmbedding.getPost().getId(),
                        postEmbedding.getEmbeddingModel(),
                        parseVector(postEmbedding.getEmbeddingJson())));

        Map<Long, List<PostEmbeddingChunk>> chunksByPostId = postEmbeddingChunkRepository
                .findAllByEmbeddingModel(embeddingModel)
                .stream()
                .filter((chunk) -> chunk.getPost().getId() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        (chunk) -> chunk.getPost().getId(),
                        LinkedHashMap::new,
                        java.util.stream.Collectors.toList()));

        int syncedChunkPostCount = 0;
        int syncedChunkVectorCount = 0;
        for (Map.Entry<Long, List<PostEmbeddingChunk>> entry : chunksByPostId.entrySet()) {
            if (syncedChunkPostCount >= limit) {
                break;
            }

            List<ChunkVectorPoint> chunkVectorPoints = entry.getValue()
                    .stream()
                    .map((chunk) -> new ChunkVectorPoint(
                            chunk.getChunkIndex(),
                            chunk.getEmbeddingModel(),
                            parseVector(chunk.getEmbeddingJson())))
                    .toList();

            qdrantVectorStoreClient.replacePostChunks(entry.getKey(), chunkVectorPoints);
            syncedChunkPostCount++;
            syncedChunkVectorCount += chunkVectorPoints.size();
        }

        return new SyncResult(
                embeddingModel,
                true,
                postEmbeddings.size(),
                syncedChunkPostCount,
                syncedChunkVectorCount);
    }

    private List<Double> parseVector(String embeddingJson) {
        try {
            return objectMapper.readValue(embeddingJson, EMBEDDING_VECTOR_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored embedding vector could not be parsed.", exception);
        }
    }

    private static int normalizeLimit(int requestedLimit) {
        return Math.min(Math.max(requestedLimit, 1), MAX_SYNC_POSTS);
    }

    public record SyncResult(
            String embeddingModel,
            boolean vectorStoreEnabled,
            int syncedPostVectorCount,
            int syncedChunkPostCount,
            int syncedChunkVectorCount) {
    }
}
