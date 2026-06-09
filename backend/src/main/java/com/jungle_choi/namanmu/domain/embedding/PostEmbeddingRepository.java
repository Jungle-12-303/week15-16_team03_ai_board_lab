package com.jungle_choi.namanmu.domain.embedding;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostEmbeddingRepository extends JpaRepository<PostEmbedding, Long> {

    Optional<PostEmbedding> findByPost_Id(Long postId);

    boolean existsByPost_IdAndSourceHash(Long postId, String sourceHash);

    List<PostEmbedding> findAllByEmbeddingModel(String embeddingModel);
}
