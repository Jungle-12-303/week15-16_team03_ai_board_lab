package com.example.aiknowledgeboard.ai.common;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "ai_logs")
public class AiLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "feature_type", nullable = false)
    private String featureType;

    @Column(name = "input_summary", columnDefinition = "TEXT")
    private String inputSummary;

    @Column(name = "output_summary", columnDefinition = "TEXT")
    private String outputSummary;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AiLog() {
    }

    public AiLog(String featureType, String inputSummary, String outputSummary, boolean success, String errorMessage) {
        this.featureType = featureType;
        this.inputSummary = inputSummary;
        this.outputSummary = outputSummary;
        this.success = success;
        this.errorMessage = errorMessage;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}
