package com.elmlite.platform.controller;

import com.elmlite.platform.common.ApiResponse;
import com.elmlite.platform.service.WalletRechargeService;
import com.elmlite.platform.service.WalletWithdrawalService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wallets/me/withdrawals")
public class WalletWithdrawalController {

    private final WalletWithdrawalService service;

    public WalletWithdrawalController(WalletWithdrawalService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<WalletWithdrawalService.WithdrawalView> withdraw(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody WithdrawalRequest request) {

        return ApiResponse.success(service.withdraw(
                jwt.getClaimAsString("accountType"),
                Long.parseLong(jwt.getSubject()),
                request.amountCent(),
                request.requestId()));
    }

    public record WithdrawalRequest(
            @NotNull @Positive @Max(WalletRechargeService.MAX_AMOUNT_CENT) Long amountCent,
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,64}") String requestId) {
    }
}
