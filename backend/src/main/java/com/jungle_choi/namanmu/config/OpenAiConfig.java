package com.jungle_choi.namanmu.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(OpenAiProperties.class)
public class OpenAiConfig {

    @Bean
    public RestClient openAiRestClient(
            RestClient.Builder restClientBuilder,
            OpenAiProperties openAiProperties) {
        return restClientBuilder
                .baseUrl(openAiProperties.baseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(createRequestFactory(openAiProperties))
                .build();
    }

    private static SimpleClientHttpRequestFactory createRequestFactory(
            OpenAiProperties openAiProperties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(openAiProperties.requestTimeout());
        requestFactory.setReadTimeout(openAiProperties.requestTimeout());

        return requestFactory;
    }
}
