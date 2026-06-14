package com.example.assistant.dto.billing;

public record TokenBalanceDTO(
        long balanceTokens,
        long totalRechargedTokens,
        long totalUsedTokens
) {}
