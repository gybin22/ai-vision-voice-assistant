package com.example.assistant.service;

import com.example.assistant.config.AssistantProperties;
import com.example.assistant.model.CachedVisionAnswer;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class CacheService {
    private final Cache<String, CachedVisionAnswer> cache;

    public CacheService(AssistantProperties properties) {
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(properties.getCost().getCacheTtlSeconds()))
                .maximumSize(10_000)
                .build();
    }

    public CachedVisionAnswer get(String key) {
        return cache.getIfPresent(key);
    }

    public void put(String key, CachedVisionAnswer value) {
        cache.put(key, value);
    }
}
