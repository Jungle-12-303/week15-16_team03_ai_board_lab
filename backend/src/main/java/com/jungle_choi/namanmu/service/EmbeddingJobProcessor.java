package com.jungle_choi.namanmu.service;

import com.jungle_choi.namanmu.domain.embedding.EmbeddingJob;
import com.jungle_choi.namanmu.domain.embedding.EmbeddingJobRepository;
import com.jungle_choi.namanmu.domain.embedding.EmbeddingJobStatus;
import com.jungle_choi.namanmu.domain.embedding.PostEmbeddingRepository;
import com.jungle_choi.namanmu.service.OpenAiEmbeddingClient.EmbeddingResult;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class EmbeddingJobProcessor {

    private static final int MAX_ERROR_MESSAGE_LENGTH = 1000;

    private final EmbeddingJobRepository embeddingJobRepository;
    private final PostEmbeddingRepository postEmbeddingRepository;
    private final PostEmbeddingTextBuilder postEmbeddingTextBuilder;
    private final OpenAiEmbeddingClient openAiEmbeddingClient;
    private final PostEmbeddingService postEmbeddingService;
    private final TransactionTemplate transactionTemplate;

    public EmbeddingJobProcessor(
            EmbeddingJobRepository embeddingJobRepository,
            PostEmbeddingRepository postEmbeddingRepository,
            PostEmbeddingTextBuilder postEmbeddingTextBuilder,
            OpenAiEmbeddingClient openAiEmbeddingClient,
            PostEmbeddingService postEmbeddingService,
            TransactionTemplate transactionTemplate) {
        this.embeddingJobRepository = embeddingJobRepository;
        this.postEmbeddingRepository = postEmbeddingRepository;
        this.postEmbeddingTextBuilder = postEmbeddingTextBuilder;
        this.openAiEmbeddingClient = openAiEmbeddingClient;
        this.postEmbeddingService = postEmbeddingService;
        this.transactionTemplate = transactionTemplate;
    }

    public ProcessEmbeddingJobResult processOnePendingJob() {
        return claimPendingJob()
                .map(this::processClaimedJob)
                .orElseGet(ProcessEmbeddingJobResult::noJob);
    }

    private Optional<ClaimedEmbeddingJob> claimPendingJob() {
        return transactionTemplate.execute((status) ->
                embeddingJobRepository.findFirstByStatusOrderByCreatedAtAsc(EmbeddingJobStatus.PENDING)
                        .map((embeddingJob) -> {
                            embeddingJob.markProcessing();
                            return new ClaimedEmbeddingJob(
                                    embeddingJob.getId(),
                                    embeddingJob.getPost().getId());
                        }));
    }

    private ProcessEmbeddingJobResult processClaimedJob(ClaimedEmbeddingJob claimedJob) {
        try {
            String sourceText = postEmbeddingTextBuilder.build(claimedJob.postId());

            if (isAlreadyUpToDate(claimedJob.postId(), sourceText)) {
                markCompleted(claimedJob.jobId());
                return ProcessEmbeddingJobResult.completed(
                        claimedJob.jobId(),
                        claimedJob.postId(),
                        "Embedding is already up to date.");
            }

            EmbeddingResult embeddingResult = openAiEmbeddingClient.createEmbedding(sourceText);
            postEmbeddingService.saveOrReplace(claimedJob.postId(), sourceText, embeddingResult);
            markCompleted(claimedJob.jobId());

            return ProcessEmbeddingJobResult.completed(
                    claimedJob.jobId(),
                    claimedJob.postId(),
                    "Embedding was created.");
        } catch (Exception exception) {
            markFailed(claimedJob.jobId(), exception);

            return ProcessEmbeddingJobResult.failed(
                    claimedJob.jobId(),
                    claimedJob.postId(),
                    toErrorMessage(exception));
        }
    }

    private boolean isAlreadyUpToDate(Long postId, String sourceText) {
        return postEmbeddingRepository.existsByPost_IdAndSourceHash(
                postId,
                postEmbeddingService.sourceHash(sourceText));
    }

    private void markCompleted(Long jobId) {
        transactionTemplate.executeWithoutResult((status) ->
                embeddingJobRepository.findById(jobId)
                        .ifPresent(EmbeddingJob::markCompleted));
    }

    private void markFailed(Long jobId, Exception exception) {
        transactionTemplate.executeWithoutResult((status) ->
                embeddingJobRepository.findById(jobId)
                        .ifPresent((embeddingJob) ->
                                embeddingJob.markFailed(toErrorMessage(exception))));
    }

    private static String toErrorMessage(Exception exception) {
        String message = exception.getMessage();

        if (message == null || message.isBlank()) {
            message = exception.getClass().getSimpleName();
        }

        if (message.length() <= MAX_ERROR_MESSAGE_LENGTH) {
            return message;
        }

        return message.substring(0, MAX_ERROR_MESSAGE_LENGTH);
    }

    private record ClaimedEmbeddingJob(Long jobId, Long postId) {
    }

    public record ProcessEmbeddingJobResult(
            boolean processed,
            boolean succeeded,
            Long jobId,
            Long postId,
            String message) {

        public static ProcessEmbeddingJobResult noJob() {
            return new ProcessEmbeddingJobResult(
                    false,
                    true,
                    null,
                    null,
                    "No pending embedding job.");
        }

        public static ProcessEmbeddingJobResult completed(
                Long jobId,
                Long postId,
                String message) {
            return new ProcessEmbeddingJobResult(true, true, jobId, postId, message);
        }

        public static ProcessEmbeddingJobResult failed(
                Long jobId,
                Long postId,
                String message) {
            return new ProcessEmbeddingJobResult(true, false, jobId, postId, message);
        }
    }
}
