package com.jungle_choi.namanmu.domain.embedding;

import com.jungle_choi.namanmu.domain.post.Post;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "embedding_jobs")
public class EmbeddingJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmbeddingJobStatus status = EmbeddingJobStatus.PENDING;

    @Column(nullable = false)
    private int attemptCount = 0;

    @Column(length = 1000)
    private String errorMessage;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public static EmbeddingJob createPending(Post post) {
        EmbeddingJob embeddingJob = new EmbeddingJob();
        embeddingJob.post = post;
        embeddingJob.status = EmbeddingJobStatus.PENDING;
        embeddingJob.attemptCount = 0;
        return embeddingJob;
    }

    public void markProcessing() {
        this.status = EmbeddingJobStatus.PROCESSING;
        this.attemptCount += 1;
        this.errorMessage = null;
        this.startedAt = LocalDateTime.now();
        this.completedAt = null;
    }

    public void markCompleted() {
        this.status = EmbeddingJobStatus.COMPLETED;
        this.errorMessage = null;
        this.completedAt = LocalDateTime.now();
    }

    public void markFailed(String errorMessage) {
        this.status = EmbeddingJobStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Post getPost() {
        return post;
    }

    public EmbeddingJobStatus getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
