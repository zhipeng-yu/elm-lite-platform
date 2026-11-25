package com.elmlite.platform.controller;

import com.elmlite.platform.common.ApiResponse;
import com.elmlite.platform.service.WalletCreditService;
import com.elmlite.platform.service.WalletRechargeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class WalletCreditController {

    private final WalletCreditService service;

    public WalletCreditController(WalletCreditService service) {
        this.service = service;
    }

    @GetMapping("/wallets/me/credit")
    public ApiResponse<WalletCreditService.CreditView> get(
            @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(service.get(
                jwt.getClaimAsString("accountType"),
                Long.parseLong(jwt.getSubject())));
    }

    @PutMapping("/admin/wallets/{walletId}/credit")
    public ApiResponse<WalletCreditService.CreditView> configure(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable long walletId,
            @Valid @RequestBody CreditRequest request) {
        return ApiResponse.success(service.configure(
                Long.parseLong(jwt.getSubject()),
                walletId,
                request.creditLimitCent(),
                request.repaymentDays(),
                request.dailyRateBasisPoints()));
    }

    public record CreditRequest(
            @NotNull @Min(0) @Max(WalletRechargeService.MAX_AMOUNT_CENT) Long creditLimitCent,
            @NotNull @Min(1) @Max(3650) Integer repaymentDays,
            @NotNull @Min(0) @Max(10000) Integer dailyRateBasisPoints) {
    }
}
