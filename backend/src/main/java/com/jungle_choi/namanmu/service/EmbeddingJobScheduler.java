package com.jungle_choi.namanmu.service;

import com.jungle_choi.namanmu.config.EmbeddingWorkerProperties;
import com.jungle_choi.namanmu.service.EmbeddingJobProcessor.ProcessEmbeddingJobsResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EmbeddingJobScheduler {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingJobScheduler.class);

    private final EmbeddingJobProcessor embeddingJobProcessor;
    private final OpenAiEmbeddingClient openAiEmbeddingClient;
    private final EmbeddingWorkerProperties embeddingWorkerProperties;

    public EmbeddingJobScheduler(
            EmbeddingJobProcessor embeddingJobProcessor,
            OpenAiEmbeddingClient openAiEmbeddingClient,
            EmbeddingWorkerProperties embeddingWorkerProperties) {
        this.embeddingJobProcessor = embeddingJobProcessor;
        this.openAiEmbeddingClient = openAiEmbeddingClient;
        this.embeddingWorkerProperties = embeddingWorkerProperties;
    }

    @Scheduled(fixedDelayString = "${app.embedding-worker.fixed-delay-millis:30000}")
    public void processPendingEmbeddingJobs() {
        if (!embeddingWorkerProperties.enabled() || !openAiEmbeddingClient.isConfigured()) {
            return;
        }

        ProcessEmbeddingJobsResult result = embeddingJobProcessor.processPendingJobs(
                embeddingWorkerProperties.normalizedBatchSize());

        if (result.processedCount() > 0 || result.failedCount() > 0) {
            log.info(
                    "Embedding worker processed jobs. requested={}, processed={}, succeeded={}, failed={}",
                    result.requestedLimit(),
                    result.processedCount(),
                    result.succeededCount(),
                    result.failedCount());
        }
    }
}
