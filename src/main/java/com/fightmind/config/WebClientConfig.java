package com.fightmind.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class WebClientConfig {

    @Value("${python.service.url:http://localhost:8000}")
    private String pythonServiceUrl;

    @Value("${python.service.connectTimeoutMs:5000}")
    private int connectTimeoutMs;

    @Value("${python.service.readTimeoutMs:30000}")
    private int readTimeoutMs;

    @Bean
    public RestClient pythonAiRestClient() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = 
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
                
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);

        return RestClient.builder()
                .baseUrl(pythonServiceUrl)
                .requestFactory(factory)
                .build();
    }
}
