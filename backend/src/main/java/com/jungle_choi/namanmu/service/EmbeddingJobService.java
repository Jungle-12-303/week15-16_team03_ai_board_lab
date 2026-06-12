package com.jungle_choi.namanmu.service;

import com.jungle_choi.namanmu.domain.embedding.EmbeddingJob;
import com.jungle_choi.namanmu.domain.embedding.EmbeddingJobRepository;
import com.jungle_choi.namanmu.domain.embedding.EmbeddingJobStatus;
import com.jungle_choi.namanmu.domain.post.Post;
import com.jungle_choi.namanmu.domain.post.PostRepository;
import com.jungle_choi.namanmu.domain.post.PostStatus;
import com.jungle_choi.namanmu.config.OpenAiProperties;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmbeddingJobService {

    private static final int MAX_ENQUEUE_LIMIT = 100;
    private static final List<EmbeddingJobStatus> OPEN_JOB_STATUSES =
            List.of(EmbeddingJobStatus.PENDING, EmbeddingJobStatus.PROCESSING);

    private final EmbeddingJobRepository embeddingJobRepository;
    private final PostRepository postRepository;
    private final OpenAiProperties openAiProperties;

    public EmbeddingJobService(
            EmbeddingJobRepository embeddingJobRepository,
            PostRepository postRepository,
            OpenAiProperties openAiProperties) {
        this.embeddingJobRepository = embeddingJobRepository;
        this.postRepository = postRepository;
        this.openAiProperties = openAiProperties;
    }

    @Transactional
    public void enqueuePostEmbedding(Post post) {
        enqueuePostEmbeddingIfNeeded(post);
    }

    @Transactional
    public EnqueueEmbeddingJobsResult enqueuePostsMissingEmbeddingChunks(int requestedLimit) {
        int limit = normalizeLimit(requestedLimit);
        List<Post> posts = postRepository.findPostsMissingEmbeddingChunks(
                PostStatus.PUBLISHED,
                openAiProperties.embeddingModel(),
                OPEN_JOB_STATUSES,
                PageRequest.of(0, limit));
        List<Long> enqueuedPostIds = new ArrayList<>();

        for (Post post : posts) {
            if (enqueuePostEmbeddingIfNeeded(post)) {
                enqueuedPostIds.add(post.getId());
            }
        }

        return new EnqueueEmbeddingJobsResult(
                limit,
                enqueuedPostIds.size(),
                enqueuedPostIds);
    }

    private boolean enqueuePostEmbeddingIfNeeded(Post post) {
        boolean hasOpenJob = embeddingJobRepository.existsByPost_IdAndStatusIn(
                post.getId(),
                OPEN_JOB_STATUSES);

        if (hasOpenJob) {
            return false;
        }

        embeddingJobRepository.save(EmbeddingJob.createPending(post));
        return true;
    }

    private static int normalizeLimit(int requestedLimit) {
        return Math.min(Math.max(requestedLimit, 1), MAX_ENQUEUE_LIMIT);
    }

    public record EnqueueEmbeddingJobsResult(
            int requestedLimit,
            int enqueuedCount,
            List<Long> postIds) {
    }
}
