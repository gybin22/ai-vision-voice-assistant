package com.example.assistant;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EnvCheck {

    private final Environment env;

    @PostConstruct
    public void check() {
        System.out.println("========== Config Check ==========");
        System.out.println("Active profiles: " + String.join(",", env.getActiveProfiles()));
        System.out.println("DASHSCOPE_API_KEY loaded: " +
                (env.getProperty("DASHSCOPE_API_KEY") != null && !env.getProperty("DASHSCOPE_API_KEY").isBlank()));
        System.out.println("DASHSCOPE_BASE_URL: " + env.getProperty("DASHSCOPE_BASE_URL"));
        System.out.println("ASSISTANT_MODEL_MODEL_NAME: " + env.getProperty("ASSISTANT_MODEL_MODEL_NAME"));
        System.out.println("ASSISTANT_CORS_ALLOWED_ORIGINS: " + env.getProperty("ASSISTANT_CORS_ALLOWED_ORIGINS"));
        System.out.println("==================================");
    }
}