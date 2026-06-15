package com.example.aiknowledgeboard.ai.agent;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "agent_memory")
public class AgentMemory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Column(name = "memory_key", nullable = false)
    private String memoryKey;

    @Column(name = "memory_value", nullable = false, columnDefinition = "TEXT")
    private String memoryValue;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AgentMemory() {
    }

    public AgentMemory(Long userId, String sessionId, String memoryKey, String memoryValue) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.memoryKey = memoryKey;
        this.memoryValue = memoryValue;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}
