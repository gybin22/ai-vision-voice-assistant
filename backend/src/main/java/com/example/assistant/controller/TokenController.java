package com.example.assistant.controller;

import com.example.assistant.dto.billing.TokenBalanceDTO;
import com.example.assistant.dto.billing.TokenRechargeRequest;
import com.example.assistant.dto.billing.TokenRechargeResponse;
import com.example.assistant.security.UserPrincipal;
import com.example.assistant.service.billing.TokenBillingService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tokens")
public class TokenController {
    private final TokenBillingService tokenBillingService;

    public TokenController(TokenBillingService tokenBillingService) {
        this.tokenBillingService = tokenBillingService;
    }

    @GetMapping("/balance")
    public TokenBalanceDTO balance(@AuthenticationPrincipal UserPrincipal principal) {
        return tokenBillingService.getBalance(principal.getId());
    }

    /**
     * 开发版模拟充值接口。正式支付接入后，应改为由支付回调确认充值，不能直接信任前端传入的金额。
     */
    @PostMapping("/recharge")
    public TokenRechargeResponse recharge(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TokenRechargeRequest request
    ) {
        return tokenBillingService.recharge(principal.getId(), request.amountTokens());
    }
}
