package com.jungle_choi.namanmu.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(QdrantProperties.class)
public class QdrantConfig {

    @Bean
    public RestClient qdrantRestClient(
            RestClient.Builder restClientBuilder,
            QdrantProperties qdrantProperties) {
        return restClientBuilder
                .baseUrl(qdrantProperties.baseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(createRequestFactory(qdrantProperties))
                .build();
    }

    private static SimpleClientHttpRequestFactory createRequestFactory(
            QdrantProperties qdrantProperties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(qdrantProperties.requestTimeout());
        requestFactory.setReadTimeout(qdrantProperties.requestTimeout());

        return requestFactory;
    }
}
