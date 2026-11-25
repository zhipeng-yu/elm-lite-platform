package com.elmlite.platform.controller;

import com.elmlite.platform.common.ApiResponse;
import com.elmlite.platform.service.WalletRechargeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/wallet/recharge-rule")
public class WalletRechargeRuleController {
    private final WalletRechargeService service;
    public WalletRechargeRuleController(WalletRechargeService service) { this.service = service; }

    @GetMapping
    public ApiResponse<WalletRechargeService.RuleView> get(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(service.getRule(Long.parseLong(jwt.getSubject())));
    }

    @PutMapping
    public ApiResponse<WalletRechargeService.RuleView> update(@AuthenticationPrincipal Jwt jwt,
                                                              @Valid @RequestBody RuleRequest request) {
        return ApiResponse.success(service.updateRule(Long.parseLong(jwt.getSubject()),
                request.thresholdCent(), request.rewardCent(), request.enabled()));
    }

    public record RuleRequest(
            @NotNull @Positive @Max(WalletRechargeService.MAX_AMOUNT_CENT) Long thresholdCent,
            @NotNull @PositiveOrZero @Max(WalletRechargeService.MAX_AMOUNT_CENT) Long rewardCent,
            @NotNull Boolean enabled) {}
}
