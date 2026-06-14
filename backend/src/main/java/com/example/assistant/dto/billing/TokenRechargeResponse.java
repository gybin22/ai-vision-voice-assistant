package com.example.assistant.dto.billing;

public record TokenRechargeResponse(
        TokenBalanceDTO balance,
        long addedTokens,
        Long transactionId
) {}
