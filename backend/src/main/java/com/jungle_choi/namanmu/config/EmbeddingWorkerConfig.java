package com.jungle_choi.namanmu.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(EmbeddingWorkerProperties.class)
public class EmbeddingWorkerConfig {
}
