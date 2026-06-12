package com.jungle_choi.namanmu.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CorpusImportProperties.class)
public class CorpusImportConfig {
}
