package com.jungle_choi.namanmu.service.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.jungle_choi.namanmu.config.QdrantProperties;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class QdrantVectorStoreClient {

    private static final Logger log = LoggerFactory.getLogger(QdrantVectorStoreClient.class);

    private final RestClient qdrantRestClient;
    private final QdrantProperties qdrantProperties;
    private final Set<String> preparedCollections = ConcurrentHashMap.newKeySet();

    public QdrantVectorStoreClient(
            @Qualifier("qdrantRestClient") RestClient qdrantRestClient,
            QdrantProperties qdrantProperties) {
        this.qdrantRestClient = qdrantRestClient;
        this.qdrantProperties = qdrantProperties;
    }

    public boolean isEnabled() {
        return qdrantProperties.enabled();
    }

    public void upsertPost(
            Long postId,
            String embeddingModel,
            List<Double> embedding) {
        if (!isUsableEmbedding(postId, embedding)) {
            return;
        }

        upsertPoints(
                qdrantProperties.postCollection(),
                embedding.size(),
                List.of(Map.of(
                        "id", postId,
                        "vector", embedding,
                        "payload", Map.of(
                                "postId", postId,
                                "embeddingModel", embeddingModel,
                                "type", "post"))));
    }

    public void replacePostChunks(
            Long postId,
            List<ChunkVectorPoint> chunkVectorPoints) {
        if (!qdrantProperties.enabled() || postId == null || chunkVectorPoints.isEmpty()) {
            return;
        }

        deleteChunksForPost(postId);

        List<Map<String, Object>> points = chunkVectorPoints.stream()
                .filter((chunk) -> isUsableEmbedding(postId, chunk.embedding()))
                .map((chunk) -> Map.<String, Object>of(
                        "id", chunkPointId(postId, chunk.chunkIndex()),
                        "vector", chunk.embedding(),
                        "payload", Map.of(
                                "postId", postId,
                                "chunkIndex", chunk.chunkIndex(),
                                "embeddingModel", chunk.embeddingModel(),
                                "type", "chunk")))
                .toList();

        if (points.isEmpty()) {
            return;
        }

        int dimensions = chunkVectorPoints.getFirst().embedding().size();
        upsertPoints(qdrantProperties.chunkCollection(), dimensions, points);
    }

    public Optional<List<Long>> searchPostIds(
            List<Double> queryEmbedding,
            String embeddingModel) {
        return searchPostIds(
                qdrantProperties.postCollection(),
                queryEmbedding,
                embeddingModel,
                qdrantProperties.normalizedSearchLimit());
    }

    public Optional<List<Long>> searchChunkPostIds(
            List<Double> queryEmbedding,
            String embeddingModel) {
        return searchPostIds(
                qdrantProperties.chunkCollection(),
                queryEmbedding,
                embeddingModel,
                qdrantProperties.normalizedSearchLimit());
    }

    private Optional<List<Long>> searchPostIds(
            String collection,
            List<Double> queryEmbedding,
            String embeddingModel,
            int limit) {
        if (!qdrantProperties.enabled() || queryEmbedding == null || queryEmbedding.isEmpty()) {
            return Optional.empty();
        }

        try {
            JsonNode response = qdrantRestClient.post()
                    .uri("/collections/{collection}/points/query", collection)
                    .body(Map.of(
                            "query", queryEmbedding,
                            "filter", embeddingModelFilter(embeddingModel),
                            "limit", limit,
                            "with_payload", true))
                    .retrieve()
                    .body(JsonNode.class);

            return Optional.of(extractPostIds(response));
        } catch (Exception exception) {
            log.warn(
                    "Qdrant search failed. collection={}, fallback=mysql-full-scan",
                    collection,
                    exception);
            return Optional.empty();
        }
    }

    private void upsertPoints(
            String collection,
            int dimensions,
            List<Map<String, Object>> points) {
        if (!qdrantProperties.enabled() || points.isEmpty()) {
            return;
        }

        try {
            ensureCollection(collection, dimensions);
            qdrantRestClient.put()
                    .uri("/collections/{collection}/points?wait=true", collection)
                    .body(Map.of("points", points))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception exception) {
            log.warn(
                    "Qdrant upsert failed. collection={}, points={}, fallback=mysql-only",
                    collection,
                    points.size(),
                    exception);
        }
    }

    private void deleteChunksForPost(Long postId) {
        try {
            qdrantRestClient.post()
                    .uri("/collections/{collection}/points/delete?wait=true",
                            qdrantProperties.chunkCollection())
                    .body(Map.of(
                            "filter", Map.of(
                                    "must", List.of(Map.of(
                                            "key", "postId",
                                            "match", Map.of("value", postId))))))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() != 404) {
                log.warn("Qdrant chunk cleanup failed. postId={}", postId, exception);
            }
        } catch (Exception exception) {
            log.warn("Qdrant chunk cleanup failed. postId={}", postId, exception);
        }
    }

    private void ensureCollection(String collection, int dimensions) {
        if (dimensions <= 0) {
            return;
        }

        String collectionKey = "%s:%d".formatted(collection, dimensions);
        if (preparedCollections.contains(collectionKey)) {
            return;
        }

        try {
            if (!collectionExists(collection)) {
                createCollection(collection, dimensions);
            }

            preparedCollections.add(collectionKey);
        } catch (Exception exception) {
            log.warn("Qdrant collection preparation failed. collection={}", collection, exception);
        }
    }

    private boolean collectionExists(String collection) {
        try {
            qdrantRestClient.get()
                    .uri("/collections/{collection}", collection)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404) {
                return false;
            }

            throw exception;
        }
    }

    private void createCollection(String collection, int dimensions) {
        try {
            qdrantRestClient.put()
                    .uri("/collections/{collection}", collection)
                    .body(Map.of(
                            "vectors", Map.of(
                                    "size", dimensions,
                                    "distance", "Cosine")))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() != 409) {
                throw exception;
            }
        }
    }

    private static Map<String, Object> embeddingModelFilter(String embeddingModel) {
        return Map.of(
                "must", List.of(Map.of(
                        "key", "embeddingModel",
                        "match", Map.of("value", embeddingModel))));
    }

    private static List<Long> extractPostIds(JsonNode response) {
        JsonNode points = response == null
                ? null
                : response.path("result").path("points");
        if (points == null || !points.isArray()) {
            return List.of();
        }

        Set<Long> postIds = new LinkedHashSet<>();
        for (JsonNode point : points) {
            JsonNode postIdNode = point.path("payload").path("postId");
            if (postIdNode.canConvertToLong()) {
                postIds.add(postIdNode.asLong());
            }
        }

        return new ArrayList<>(postIds);
    }

    private static boolean isUsableEmbedding(Long postId, List<Double> embedding) {
        return postId != null && embedding != null && !embedding.isEmpty();
    }

    private static String chunkPointId(Long postId, int chunkIndex) {
        return UUID.nameUUIDFromBytes(
                        "post-chunk:%d:%d".formatted(postId, chunkIndex)
                                .getBytes(StandardCharsets.UTF_8))
                .toString();
    }

    public record ChunkVectorPoint(
            int chunkIndex,
            String embeddingModel,
            List<Double> embedding) {
    }
}
