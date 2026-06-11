package com.jungle_choi.namanmu.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jungle_choi.namanmu.config.OpenAiProperties;
import com.jungle_choi.namanmu.domain.embedding.PostEmbedding;
import com.jungle_choi.namanmu.domain.embedding.PostEmbeddingRepository;
import com.jungle_choi.namanmu.domain.post.Post;
import com.jungle_choi.namanmu.domain.post.PostStatus;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SimilarPostSearchService {

    private static final int MAX_LIMIT = 10;
    private static final TypeReference<List<Double>> EMBEDDING_VECTOR_TYPE = new TypeReference<>() {
    };

    private final PostEmbeddingRepository postEmbeddingRepository;
    private final OpenAiProperties openAiProperties;
    private final ObjectMapper objectMapper;

    public SimilarPostSearchService(
            PostEmbeddingRepository postEmbeddingRepository,
            OpenAiProperties openAiProperties,
            ObjectMapper objectMapper) {
        this.postEmbeddingRepository = postEmbeddingRepository;
        this.openAiProperties = openAiProperties;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<SimilarPostResult> searchSimilarPosts(
            List<Double> queryEmbedding,
            Long excludedPostId,
            int limit) {
        if (queryEmbedding == null || queryEmbedding.isEmpty()) {
            return List.of();
        }

        int normalizedLimit = normalizeLimit(limit);

        return postEmbeddingRepository.findAllByEmbeddingModel(openAiProperties.embeddingModel())
                .stream()
                .map((postEmbedding) -> toSimilarPostResult(postEmbedding, queryEmbedding, excludedPostId))
                .flatMap(Optional::stream)
                .sorted(Comparator.comparingDouble(SimilarPostResult::score).reversed())
                .limit(normalizedLimit)
                .toList();
    }

    private Optional<SimilarPostResult> toSimilarPostResult(
            PostEmbedding postEmbedding,
            List<Double> queryEmbedding,
            Long excludedPostId) {
        Post post = postEmbedding.getPost();

        if (post.getStatus() != PostStatus.PUBLISHED) {
            return Optional.empty();
        }

        if (Objects.equals(post.getId(), excludedPostId)) {
            return Optional.empty();
        }

        List<Double> storedEmbedding = parseEmbedding(postEmbedding);
        if (storedEmbedding.size() != queryEmbedding.size()) {
            return Optional.empty();
        }

        double score = cosineSimilarity(queryEmbedding, storedEmbedding);

        return Optional.of(new SimilarPostResult(
                post.getId(),
                post.getTitle(),
                post.getCategory(),
                post.getContent(),
                score));
    }

    private List<Double> parseEmbedding(PostEmbedding postEmbedding) {
        try {
            return objectMapper.readValue(
                    postEmbedding.getEmbeddingJson(),
                    EMBEDDING_VECTOR_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored embedding vector could not be parsed.", exception);
        }
    }

    static double cosineSimilarity(List<Double> left, List<Double> right) {
        if (left == null || right == null || left.size() != right.size() || left.isEmpty()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double leftMagnitude = 0.0;
        double rightMagnitude = 0.0;

        for (int index = 0; index < left.size(); index++) {
            double leftValue = left.get(index);
            double rightValue = right.get(index);

            dotProduct += leftValue * rightValue;
            leftMagnitude += leftValue * leftValue;
            rightMagnitude += rightValue * rightValue;
        }

        if (leftMagnitude == 0.0 || rightMagnitude == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(leftMagnitude) * Math.sqrt(rightMagnitude));
    }

    private static int normalizeLimit(int limit) {
        return Math.min(Math.max(limit, 1), MAX_LIMIT);
    }

    public record SimilarPostResult(
            Long postId,
            String title,
            String category,
            String content,
            double score) {
    }
}
