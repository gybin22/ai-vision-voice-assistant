package com.example.assistant.dto.billing;

public record BillingUsageDTO(
        long chargedTokens,
        long balanceAfterTokens,
        double tokenUnitPriceYuan,
        double revenueAmountYuan,
        double providerCostAmountYuan,
        double grossProfitAmountYuan
) {}
