package com.example.assistant.dto.billing;

import jakarta.validation.constraints.Min;

public record TokenRechargeRequest(
        @Min(value = 1, message = "充值 Tokens 必须大于 0")
        long amountTokens
) {}
