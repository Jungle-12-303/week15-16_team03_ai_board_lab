package com.jungle_choi.namanmu.service;

import com.jungle_choi.namanmu.domain.embedding.EmbeddingJob;
import com.jungle_choi.namanmu.domain.embedding.EmbeddingJobRepository;
import com.jungle_choi.namanmu.domain.embedding.EmbeddingJobStatus;
import com.jungle_choi.namanmu.domain.post.Post;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmbeddingJobService {

    private final EmbeddingJobRepository embeddingJobRepository;

    public EmbeddingJobService(EmbeddingJobRepository embeddingJobRepository) {
        this.embeddingJobRepository = embeddingJobRepository;
    }

    @Transactional
    public void enqueuePostEmbedding(Post post) {
        boolean hasPendingJob = embeddingJobRepository.existsByPost_IdAndStatus(
                post.getId(),
                EmbeddingJobStatus.PENDING);

        if (hasPendingJob) {
            return;
        }

        embeddingJobRepository.save(EmbeddingJob.createPending(post));
    }
}
