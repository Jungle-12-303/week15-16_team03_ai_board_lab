package com.jungle_choi.namanmu.domain.embedding;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostEmbeddingChunkRepository extends JpaRepository<PostEmbeddingChunk, Long> {

    void deleteAllByPost_Id(Long postId);

    List<PostEmbeddingChunk> findAllByEmbeddingModel(String embeddingModel);
}
