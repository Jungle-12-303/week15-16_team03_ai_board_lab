package com.jungle_choi.namanmu.domain.embedding;

import static org.assertj.core.api.Assertions.assertThat;

import com.jungle_choi.namanmu.domain.post.Post;
import com.jungle_choi.namanmu.domain.user.User;
import org.junit.jupiter.api.Test;

class EmbeddingJobTest {

    @Test
    void failedJobReturnsToPendingUntilMaxAttempts() {
        EmbeddingJob embeddingJob = createEmbeddingJob();

        embeddingJob.markProcessing();
        boolean willRetry = embeddingJob.markFailedOrRetry("temporary failure", 3);

        assertThat(willRetry).isTrue();
        assertThat(embeddingJob.getStatus()).isEqualTo(EmbeddingJobStatus.PENDING);
        assertThat(embeddingJob.getAttemptCount()).isEqualTo(1);
        assertThat(embeddingJob.getErrorMessage()).isEqualTo("temporary failure");
    }

    @Test
    void failedJobBecomesFinalFailedWhenMaxAttemptsIsReached() {
        EmbeddingJob embeddingJob = createEmbeddingJob();

        embeddingJob.markProcessing();
        embeddingJob.markFailedOrRetry("first failure", 3);
        embeddingJob.markProcessing();
        embeddingJob.markFailedOrRetry("second failure", 3);
        embeddingJob.markProcessing();
        boolean willRetry = embeddingJob.markFailedOrRetry("final failure", 3);

        assertThat(willRetry).isFalse();
        assertThat(embeddingJob.getStatus()).isEqualTo(EmbeddingJobStatus.FAILED);
        assertThat(embeddingJob.getAttemptCount()).isEqualTo(3);
        assertThat(embeddingJob.getErrorMessage()).isEqualTo("final failure");
    }

    private static EmbeddingJob createEmbeddingJob() {
        User user = User.createLocalUser("embedding-test-user");
        Post post = Post.create(
                user,
                "Learning",
                "Embedding retry",
                "Testing retry state transitions.");

        return EmbeddingJob.createPending(post);
    }
}
