package com.example.assistant.entity.billing;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "token_transactions",
        indexes = {
                @Index(name = "idx_token_transactions_user_created", columnList = "user_id, created_at"),
                @Index(name = "idx_token_transactions_request_id", columnList = "request_id")
        }
)
public class TokenTransactionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 30)
    private String type;

    @Column(name = "amount_tokens", nullable = false)
    private long amountTokens;

    @Column(name = "balance_after_tokens", nullable = false)
    private long balanceAfterTokens;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "request_id", length = 64)
    private String requestId;

    @Column(length = 255)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getAmountTokens() {
        return amountTokens;
    }

    public void setAmountTokens(long amountTokens) {
        this.amountTokens = amountTokens;
    }

    public long getBalanceAfterTokens() {
        return balanceAfterTokens;
    }

    public void setBalanceAfterTokens(long balanceAfterTokens) {
        this.balanceAfterTokens = balanceAfterTokens;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
