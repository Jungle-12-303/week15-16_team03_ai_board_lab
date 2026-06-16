package com.jungle_choi.namanmu.domain.embedding;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostEmbeddingChunkRepository extends JpaRepository<PostEmbeddingChunk, Long> {

    void deleteAllByPost_Id(Long postId);

    long countByPost_IdAndEmbeddingModel(Long postId, String embeddingModel);

    boolean existsByPost_IdAndChunkIndexAndEmbeddingModelAndSourceHash(
            Long postId,
            int chunkIndex,
            String embeddingModel,
            String sourceHash);

    List<PostEmbeddingChunk> findAllByEmbeddingModel(String embeddingModel);

    List<PostEmbeddingChunk> findAllByEmbeddingModelAndPost_IdIn(
            String embeddingModel,
            Collection<Long> postIds);
}
