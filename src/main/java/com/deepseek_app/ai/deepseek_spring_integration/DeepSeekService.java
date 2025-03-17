package com.deepseek_app.ai.deepseek_spring_integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import java.io.IOException;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;

@Service
@Slf4j
public class DeepSeekService {

    private final CloseableHttpClient httpClient;
    private final HttpPost httpPost;

    public DeepSeekService(CloseableHttpClient httpClient, HttpPost httpPost) {
        this.httpClient = httpClient;
        this.httpPost = httpPost;
    }

    public String generateText(String prompt) throws IOException {
        String requestBody = String.format("""
            {
                "model": "deepseek-chat",
                "messages": [
                    {"role": "user", "content": "%s"}
                ]
            }
            """, prompt);

        try {
            httpPost.setEntity(new StringEntity(requestBody));
            CloseableHttpResponse response = httpClient.execute(httpPost);
            return EntityUtils.toString(response.getEntity());
        } catch (IOException e) {
            log.error("API request failed: {}", e.getMessage());
            throw e;
        }
    }
}
