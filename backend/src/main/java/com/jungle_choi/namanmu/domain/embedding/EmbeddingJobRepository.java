package com.jungle_choi.namanmu.domain.embedding;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmbeddingJobRepository extends JpaRepository<EmbeddingJob, Long> {

    Optional<EmbeddingJob> findFirstByStatusOrderByCreatedAtAsc(EmbeddingJobStatus status);

    boolean existsByPost_IdAndStatus(Long postId, EmbeddingJobStatus status);

    boolean existsByPost_IdAndStatusIn(Long postId, List<EmbeddingJobStatus> statuses);

    long countByStatus(EmbeddingJobStatus status);

    List<EmbeddingJob> findAllByPost_IdOrderByCreatedAtDesc(Long postId);
}
