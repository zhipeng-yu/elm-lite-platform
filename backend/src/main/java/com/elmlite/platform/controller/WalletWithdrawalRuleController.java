package com.elmlite.platform.controller;

import com.elmlite.platform.common.ApiResponse;
import com.elmlite.platform.service.WalletWithdrawalRuleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/wallet/withdrawal-rule")
public class WalletWithdrawalRuleController {

    private final WalletWithdrawalRuleService service;

    public WalletWithdrawalRuleController(WalletWithdrawalRuleService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<WalletWithdrawalRuleService.RuleView> get(
            @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(
                service.getRule(Long.parseLong(jwt.getSubject())));
    }

    @PutMapping
    public ApiResponse<WalletWithdrawalRuleService.RuleView> update(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody RuleRequest request) {
        return ApiResponse.success(service.updateRule(
                Long.parseLong(jwt.getSubject()),
                request.feeBasisPoints()));
    }

    public record RuleRequest(
            @NotNull @Min(0) @Max(9999) Integer feeBasisPoints) {
    }
}
