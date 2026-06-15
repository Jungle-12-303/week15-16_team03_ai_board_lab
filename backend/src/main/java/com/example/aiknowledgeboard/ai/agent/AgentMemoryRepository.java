package com.example.aiknowledgeboard.ai.agent;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentMemoryRepository extends JpaRepository<AgentMemory, Long> {
}
