package com.jungle_choi.namanmu.service.rag;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jungle_choi.namanmu.config.EmbeddingWorkerProperties;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class EmbeddingJobSchedulerTest {

    private final EmbeddingJobProcessor embeddingJobProcessor =
            Mockito.mock(EmbeddingJobProcessor.class);
    private final OpenAiEmbeddingClient openAiEmbeddingClient =
            Mockito.mock(OpenAiEmbeddingClient.class);

    @Test
    void processPendingEmbeddingJobsRunsSmallBatchWhenWorkerIsEnabled() {
        EmbeddingJobScheduler scheduler = new EmbeddingJobScheduler(
                embeddingJobProcessor,
                openAiEmbeddingClient,
                new EmbeddingWorkerProperties(true, 30000, 5));

        when(openAiEmbeddingClient.isConfigured()).thenReturn(true);
        when(embeddingJobProcessor.processPendingJobs(5))
                .thenReturn(new EmbeddingJobProcessor.ProcessEmbeddingJobsResult(
                        5,
                        0,
                        0,
                        0,
                        List.of()));

        scheduler.processPendingEmbeddingJobs();

        verify(embeddingJobProcessor).processPendingJobs(5);
    }

    @Test
    void processPendingEmbeddingJobsDoesNothingWhenOpenAiKeyIsMissing() {
        EmbeddingJobScheduler scheduler = new EmbeddingJobScheduler(
                embeddingJobProcessor,
                openAiEmbeddingClient,
                new EmbeddingWorkerProperties(true, 30000, 5));

        when(openAiEmbeddingClient.isConfigured()).thenReturn(false);

        scheduler.processPendingEmbeddingJobs();

        verify(embeddingJobProcessor, never()).processPendingJobs(5);
    }
}
