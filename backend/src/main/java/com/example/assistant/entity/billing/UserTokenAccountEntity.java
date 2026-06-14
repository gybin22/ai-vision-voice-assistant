package com.example.assistant.entity.billing;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "user_token_accounts")
public class UserTokenAccountEntity {
    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "balance_tokens", nullable = false)
    private long balanceTokens = 0;

    @Column(name = "total_recharged_tokens", nullable = false)
    private long totalRechargedTokens = 0;

    @Column(name = "total_used_tokens", nullable = false)
    private long totalUsedTokens = 0;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public long getBalanceTokens() {
        return balanceTokens;
    }

    public void setBalanceTokens(long balanceTokens) {
        this.balanceTokens = balanceTokens;
    }

    public long getTotalRechargedTokens() {
        return totalRechargedTokens;
    }

    public void setTotalRechargedTokens(long totalRechargedTokens) {
        this.totalRechargedTokens = totalRechargedTokens;
    }

    public long getTotalUsedTokens() {
        return totalUsedTokens;
    }

    public void setTotalUsedTokens(long totalUsedTokens) {
        this.totalUsedTokens = totalUsedTokens;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
