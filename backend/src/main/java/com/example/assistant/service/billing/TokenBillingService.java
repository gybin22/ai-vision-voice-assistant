package com.example.assistant.service.billing;

import com.example.assistant.config.AssistantProperties;
import com.example.assistant.dto.billing.BillingUsageDTO;
import com.example.assistant.dto.billing.TokenBalanceDTO;
import com.example.assistant.dto.billing.TokenRechargeResponse;
import com.example.assistant.entity.billing.AiRequestLogEntity;
import com.example.assistant.entity.billing.TokenTransactionEntity;
import com.example.assistant.entity.billing.UserTokenAccountEntity;
import com.example.assistant.exception.CostLimitExceededException;
import com.example.assistant.repository.billing.AiRequestLogRepository;
import com.example.assistant.repository.billing.TokenTransactionRepository;
import com.example.assistant.repository.billing.UserTokenAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class TokenBillingService {
    private final AssistantProperties properties;
    private final UserTokenAccountRepository accountRepository;
    private final TokenTransactionRepository transactionRepository;
    private final AiRequestLogRepository requestLogRepository;

    public TokenBillingService(
            AssistantProperties properties,
            UserTokenAccountRepository accountRepository,
            TokenTransactionRepository transactionRepository,
            AiRequestLogRepository requestLogRepository
    ) {
        this.properties = properties;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.requestLogRepository = requestLogRepository;
    }

    @Transactional
    public TokenBalanceDTO getBalance(Long userId) {
        return toBalanceDTO(getOrCreateAccount(userId));
    }

    @Transactional
    public TokenRechargeResponse recharge(Long userId, long amountTokens) {
        if (amountTokens <= 0) {
            throw new CostLimitExceededException("INVALID_RECHARGE_AMOUNT", "充值 Tokens 必须大于 0。 ");
        }
        if (amountTokens > properties.getBilling().getMaxDemoRechargeTokens()) {
            throw new CostLimitExceededException(
                    "RECHARGE_AMOUNT_TOO_LARGE",
                    "单次模拟充值最多允许 " + properties.getBilling().getMaxDemoRechargeTokens() + " Tokens。"
            );
        }

        UserTokenAccountEntity account = getOrCreateAccountForUpdate(userId);
        account.setBalanceTokens(safeAdd(account.getBalanceTokens(), amountTokens));
        account.setTotalRechargedTokens(safeAdd(account.getTotalRechargedTokens(), amountTokens));
        accountRepository.save(account);

        TokenTransactionEntity transaction = new TokenTransactionEntity();
        transaction.setUserId(userId);
        transaction.setType("RECHARGE");
        transaction.setAmountTokens(amountTokens);
        transaction.setBalanceAfterTokens(account.getBalanceTokens());
        transaction.setStatus("CONFIRMED");
        transaction.setReason("开发版模拟充值");
        transactionRepository.save(transaction);

        return new TokenRechargeResponse(toBalanceDTO(account), amountTokens, transaction.getId());
    }

    @Transactional(readOnly = true)
    public void validateCanStartVisionRequest(Long userId) {
        UserTokenAccountEntity account = accountRepository.findById(userId).orElse(null);
        long balance = account == null ? 0 : account.getBalanceTokens();
        long required = Math.max(
                properties.getBilling().getMinStartBalanceTokens(),
                properties.getBilling().getMinVisualChargeTokens()
        );
        if (balance < required) {
            throw new CostLimitExceededException(
                    "TOKEN_BALANCE_LOW",
                    "Tokens 余额不足。当前余额 " + balance + "，至少需要 " + required + " Tokens 才能发起视频对话。"
            );
        }
    }

    @Transactional
    public BillingUsageDTO settleSuccessfulRequest(
            Long userId,
            String requestId,
            String sessionId,
            String modelName,
            int inputTokens,
            int outputTokens,
            int totalTokens,
            int frameCount,
            long imageTotalBytes,
            long latencyMs,
            boolean cached,
            double providerCostAmountYuan
    ) {
        long chargedTokens = calculateChargedTokens(inputTokens, outputTokens);
        UserTokenAccountEntity account = getOrCreateAccountForUpdate(userId);
        if (account.getBalanceTokens() < chargedTokens) {
            throw new CostLimitExceededException(
                    "TOKEN_BALANCE_LOW_AFTER_USAGE",
                    "Tokens 余额不足以结算本次请求。当前余额 " + account.getBalanceTokens() + "，本次应扣 " + chargedTokens + " Tokens。"
            );
        }

        account.setBalanceTokens(account.getBalanceTokens() - chargedTokens);
        account.setTotalUsedTokens(safeAdd(account.getTotalUsedTokens(), chargedTokens));
        accountRepository.save(account);

        TokenTransactionEntity transaction = new TokenTransactionEntity();
        transaction.setUserId(userId);
        transaction.setType("CONSUME");
        transaction.setAmountTokens(-chargedTokens);
        transaction.setBalanceAfterTokens(account.getBalanceTokens());
        transaction.setStatus("CONFIRMED");
        transaction.setRequestId(requestId);
        transaction.setReason(cached ? "AI 视频对话消耗（缓存命中）" : "AI 视频对话消耗");
        transactionRepository.save(transaction);

        BigDecimal tokenUnitPrice = properties.getBilling().getTokenUnitPriceYuan();
        BigDecimal revenue = BigDecimal.valueOf(chargedTokens)
                .multiply(tokenUnitPrice)
                .setScale(6, RoundingMode.HALF_UP);
        BigDecimal providerCost = BigDecimal.valueOf(cached ? 0.0 : providerCostAmountYuan)
                .setScale(6, RoundingMode.HALF_UP);
        BigDecimal grossProfit = revenue.subtract(providerCost).setScale(6, RoundingMode.HALF_UP);

        AiRequestLogEntity log = new AiRequestLogEntity();
        log.setUserId(userId);
        log.setRequestId(requestId);
        log.setSessionId(sessionId);
        log.setModelName(modelName);
        log.setInputTokens(inputTokens);
        log.setOutputTokens(outputTokens);
        log.setTotalTokens(totalTokens <= 0 ? inputTokens + outputTokens : totalTokens);
        log.setChargedTokens(chargedTokens);
        log.setTokenUnitPriceYuan(tokenUnitPrice);
        log.setRevenueAmountYuan(revenue);
        log.setProviderCostAmountYuan(providerCost);
        log.setGrossProfitAmountYuan(grossProfit);
        log.setFrameCount(frameCount);
        log.setImageTotalBytes(imageTotalBytes);
        log.setLatencyMs(latencyMs);
        log.setCached(cached);
        log.setStatus(cached ? "CACHED" : "SUCCESS");
        requestLogRepository.save(log);

        return new BillingUsageDTO(
                chargedTokens,
                account.getBalanceTokens(),
                tokenUnitPrice.doubleValue(),
                revenue.doubleValue(),
                providerCost.doubleValue(),
                grossProfit.doubleValue()
        );
    }

    @Transactional
    public void recordFailedRequest(
            Long userId,
            String requestId,
            String sessionId,
            String modelName,
            int frameCount,
            long imageTotalBytes,
            long latencyMs,
            String errorMessage
    ) {
        AiRequestLogEntity log = new AiRequestLogEntity();
        log.setUserId(userId);
        log.setRequestId(requestId);
        log.setSessionId(sessionId);
        log.setModelName(modelName == null || modelName.isBlank() ? "unknown" : modelName);
        log.setInputTokens(0);
        log.setOutputTokens(0);
        log.setTotalTokens(0);
        log.setChargedTokens(0);
        log.setTokenUnitPriceYuan(properties.getBilling().getTokenUnitPriceYuan());
        log.setRevenueAmountYuan(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP));
        log.setProviderCostAmountYuan(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP));
        log.setGrossProfitAmountYuan(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP));
        log.setFrameCount(frameCount);
        log.setImageTotalBytes(imageTotalBytes);
        log.setLatencyMs(latencyMs);
        log.setCached(false);
        log.setStatus("FAILED");
        log.setErrorMessage(errorMessage == null ? null : errorMessage.substring(0, Math.min(1000, errorMessage.length())));
        requestLogRepository.save(log);
    }

    public long calculateChargedTokens(int inputTokens, int outputTokens) {
        long inputPart = Math.max(0, (long) inputTokens) * properties.getBilling().getInputTokenMultiplier();
        long outputPart = Math.max(0, (long) outputTokens) * properties.getBilling().getOutputTokenMultiplier();
        long computed = safeAdd(inputPart, outputPart);
        return Math.max(properties.getBilling().getMinVisualChargeTokens(), computed);
    }


    private UserTokenAccountEntity getOrCreateAccount(Long userId) {
        return accountRepository.findById(userId).orElseGet(() -> {
            UserTokenAccountEntity account = new UserTokenAccountEntity();
            account.setUserId(userId);
            return accountRepository.save(account);
        });
    }

    private UserTokenAccountEntity getOrCreateAccountForUpdate(Long userId) {
        return accountRepository.findByUserIdForUpdate(userId).orElseGet(() -> {
            UserTokenAccountEntity account = new UserTokenAccountEntity();
            account.setUserId(userId);
            return accountRepository.saveAndFlush(account);
        });
    }

    private TokenBalanceDTO toBalanceDTO(UserTokenAccountEntity account) {
        return new TokenBalanceDTO(
                account.getBalanceTokens(),
                account.getTotalRechargedTokens(),
                account.getTotalUsedTokens()
        );
    }

    private long safeAdd(long left, long right) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException e) {
            return Long.MAX_VALUE;
        }
    }
}
