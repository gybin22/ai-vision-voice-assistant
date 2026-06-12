package com.example.assistant.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "assistant")
public class AssistantProperties {
    private Cors cors = new Cors();
    private Model model = new Model();
    private Cost cost = new Cost();

    public Cors getCors() {
        return cors;
    }

    public void setCors(Cors cors) {
        this.cors = cors;
    }

    public Model getModel() {
        return model;
    }

    public void setModel(Model model) {
        this.model = model;
    }

    public Cost getCost() {
        return cost;
    }

    public void setCost(Cost cost) {
        this.cost = cost;
    }

    public static class Cors {
        private String allowedOrigins = "http://localhost:5173";

        public String getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(String allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }

    public static class Model {
        /** mock 或 openai-compatible */
        @NotBlank
        private String provider = "mock";
        private String baseUrl = "https://api.openai.com/v1";
        private String apiKey = "";
        private String modelName = "gpt-4o-mini";
        @Min(1000)
        private int timeoutMs = 15000;

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModelName() {
            return modelName;
        }

        public void setModelName(String modelName) {
            this.modelName = modelName;
        }

        public int getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
        }
    }

    public static class Cost {
        @Min(1024)
        private long maxImageBytes = 819200;
        @Min(1)
        private int maxFrameCount = 8;
        @Min(1024)
        private long maxTotalImageBytes = 4194304;
        @Min(1)
        private int maxQuestionLength = 500;
        @Min(1)
        private int maxOutputTokens = 500;
        @Min(0)
        private long minSessionIntervalMs = 10000;
        @Min(1)
        private int maxSessionRequestsPerDay = 30;
        @Min(1)
        private int maxIpRequestsPerDay = 100;
        @Min(1)
        private long cacheTtlSeconds = 600;
        @Min(0)
        private int historyRounds = 3;

        public long getMaxImageBytes() {
            return maxImageBytes;
        }

        public void setMaxImageBytes(long maxImageBytes) {
            this.maxImageBytes = maxImageBytes;
        }

        public int getMaxFrameCount() {
            return maxFrameCount;
        }

        public void setMaxFrameCount(int maxFrameCount) {
            this.maxFrameCount = maxFrameCount;
        }

        public long getMaxTotalImageBytes() {
            return maxTotalImageBytes;
        }

        public void setMaxTotalImageBytes(long maxTotalImageBytes) {
            this.maxTotalImageBytes = maxTotalImageBytes;
        }

        public int getMaxQuestionLength() {
            return maxQuestionLength;
        }

        public void setMaxQuestionLength(int maxQuestionLength) {
            this.maxQuestionLength = maxQuestionLength;
        }

        public int getMaxOutputTokens() {
            return maxOutputTokens;
        }

        public void setMaxOutputTokens(int maxOutputTokens) {
            this.maxOutputTokens = maxOutputTokens;
        }

        public long getMinSessionIntervalMs() {
            return minSessionIntervalMs;
        }

        public void setMinSessionIntervalMs(long minSessionIntervalMs) {
            this.minSessionIntervalMs = minSessionIntervalMs;
        }

        public int getMaxSessionRequestsPerDay() {
            return maxSessionRequestsPerDay;
        }

        public void setMaxSessionRequestsPerDay(int maxSessionRequestsPerDay) {
            this.maxSessionRequestsPerDay = maxSessionRequestsPerDay;
        }

        public int getMaxIpRequestsPerDay() {
            return maxIpRequestsPerDay;
        }

        public void setMaxIpRequestsPerDay(int maxIpRequestsPerDay) {
            this.maxIpRequestsPerDay = maxIpRequestsPerDay;
        }

        public long getCacheTtlSeconds() {
            return cacheTtlSeconds;
        }

        public void setCacheTtlSeconds(long cacheTtlSeconds) {
            this.cacheTtlSeconds = cacheTtlSeconds;
        }

        public int getHistoryRounds() {
            return historyRounds;
        }

        public void setHistoryRounds(int historyRounds) {
            this.historyRounds = historyRounds;
        }
    }
}
