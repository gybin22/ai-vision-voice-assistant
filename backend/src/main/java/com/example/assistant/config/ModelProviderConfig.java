package com.example.assistant.config;

import com.example.assistant.client.MockVisionClient;
import com.example.assistant.client.MultimodalModelClient;
import com.example.assistant.client.OpenAiCompatibleVisionClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class ModelProviderConfig {
    @Bean
    public MultimodalModelClient multimodalModelClient(AssistantProperties properties) {
        String provider = properties.getModel().getProvider();
        if ("mock".equalsIgnoreCase(provider)) {
            return new MockVisionClient();
        }
        if ("openai-compatible".equalsIgnoreCase(provider)) {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(properties.getModel().getTimeoutMs());
            requestFactory.setReadTimeout(properties.getModel().getTimeoutMs());

            RestClient restClient = RestClient.builder()
                    .requestFactory(requestFactory)
                    .build();
            return new OpenAiCompatibleVisionClient(restClient, properties);
        }
        throw new IllegalArgumentException("不支持的模型 provider: " + provider);
    }
}
