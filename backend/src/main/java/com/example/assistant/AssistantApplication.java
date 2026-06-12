package com.example.assistant;

import com.example.assistant.config.AssistantProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
@EnableConfigurationProperties(AssistantProperties.class)
public class AssistantApplication {
    public static void main(String[] args) {
        SpringApplication.run(AssistantApplication.class, args);
    }
}
