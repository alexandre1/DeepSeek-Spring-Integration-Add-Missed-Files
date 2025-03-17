package com.deepseek_app.ai.deepseek_spring_integration;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DeepSeekConfig {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Bean
    public CloseableHttpClient httpClient() {
        return HttpClients.createDefault();
    }

    @Bean
    public HttpPost deepSeekRequest(@Value("${deepseek.api.url}") String apiUrl) {
        HttpPost request = new HttpPost(apiUrl);
        request.addHeader("Content-Type", "application/json");
        request.addHeader("Authorization", "Bearer " + apiKey);
        return request;
    }
}