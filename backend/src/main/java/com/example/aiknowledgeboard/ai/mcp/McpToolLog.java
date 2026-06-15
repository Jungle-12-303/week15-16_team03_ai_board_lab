package com.example.aiknowledgeboard.ai.mcp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "mcp_tool_logs")
public class McpToolLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tool_name", nullable = false)
    private String toolName;

    @Column(name = "request_summary", columnDefinition = "TEXT")
    private String requestSummary;

    @Column(name = "response_summary", columnDefinition = "TEXT")
    private String responseSummary;

    @Column(nullable = false)
    private String status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected McpToolLog() {
    }

    public McpToolLog(String toolName, String requestSummary, String responseSummary, String status, String errorMessage) {
        this.toolName = toolName;
        this.requestSummary = requestSummary;
        this.responseSummary = responseSummary;
        this.status = status;
        this.errorMessage = errorMessage;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}
