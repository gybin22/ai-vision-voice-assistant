package com.example.assistant.entity.billing;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "ai_request_logs",
        indexes = {
                @Index(name = "idx_ai_request_logs_user_created", columnList = "user_id, created_at"),
                @Index(name = "idx_ai_request_logs_model_created", columnList = "model_name, created_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ai_request_logs_request_id", columnNames = "request_id")
        }
)
public class AiRequestLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "request_id", nullable = false, length = 64)
    private String requestId;

    @Column(name = "session_id", nullable = false, length = 100)
    private String sessionId;

    @Column(name = "model_name", nullable = false, length = 80)
    private String modelName;

    @Column(name = "input_tokens", nullable = false)
    private int inputTokens;

    @Column(name = "output_tokens", nullable = false)
    private int outputTokens;

    @Column(name = "total_tokens", nullable = false)
    private int totalTokens;

    @Column(name = "charged_tokens", nullable = false)
    private long chargedTokens;

    @Column(name = "token_unit_price_yuan", nullable = false, precision = 12, scale = 8)
    private BigDecimal tokenUnitPriceYuan = BigDecimal.ZERO;

    @Column(name = "revenue_amount_yuan", nullable = false, precision = 12, scale = 6)
    private BigDecimal revenueAmountYuan = BigDecimal.ZERO;

    @Column(name = "provider_cost_amount_yuan", nullable = false, precision = 12, scale = 6)
    private BigDecimal providerCostAmountYuan = BigDecimal.ZERO;

    @Column(name = "gross_profit_amount_yuan", nullable = false, precision = 12, scale = 6)
    private BigDecimal grossProfitAmountYuan = BigDecimal.ZERO;

    @Column(name = "frame_count", nullable = false)
    private int frameCount;

    @Column(name = "image_total_bytes", nullable = false)
    private long imageTotalBytes;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "cached", nullable = false)
    private boolean cached;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public int getInputTokens() { return inputTokens; }
    public void setInputTokens(int inputTokens) { this.inputTokens = inputTokens; }
    public int getOutputTokens() { return outputTokens; }
    public void setOutputTokens(int outputTokens) { this.outputTokens = outputTokens; }
    public int getTotalTokens() { return totalTokens; }
    public void setTotalTokens(int totalTokens) { this.totalTokens = totalTokens; }
    public long getChargedTokens() { return chargedTokens; }
    public void setChargedTokens(long chargedTokens) { this.chargedTokens = chargedTokens; }
    public BigDecimal getTokenUnitPriceYuan() { return tokenUnitPriceYuan; }
    public void setTokenUnitPriceYuan(BigDecimal tokenUnitPriceYuan) { this.tokenUnitPriceYuan = tokenUnitPriceYuan; }
    public BigDecimal getRevenueAmountYuan() { return revenueAmountYuan; }
    public void setRevenueAmountYuan(BigDecimal revenueAmountYuan) { this.revenueAmountYuan = revenueAmountYuan; }
    public BigDecimal getProviderCostAmountYuan() { return providerCostAmountYuan; }
    public void setProviderCostAmountYuan(BigDecimal providerCostAmountYuan) { this.providerCostAmountYuan = providerCostAmountYuan; }
    public BigDecimal getGrossProfitAmountYuan() { return grossProfitAmountYuan; }
    public void setGrossProfitAmountYuan(BigDecimal grossProfitAmountYuan) { this.grossProfitAmountYuan = grossProfitAmountYuan; }
    public int getFrameCount() { return frameCount; }
    public void setFrameCount(int frameCount) { this.frameCount = frameCount; }
    public long getImageTotalBytes() { return imageTotalBytes; }
    public void setImageTotalBytes(long imageTotalBytes) { this.imageTotalBytes = imageTotalBytes; }
    public Long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Long latencyMs) { this.latencyMs = latencyMs; }
    public boolean isCached() { return cached; }
    public void setCached(boolean cached) { this.cached = cached; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
