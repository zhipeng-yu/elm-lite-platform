package com.elmlite.platform.controller;

import com.elmlite.platform.common.ApiResponse;
import com.elmlite.platform.service.WalletRechargeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/v1/wallets/me")
public class WalletRechargeController {
    private final WalletRechargeService service;
    public WalletRechargeController(WalletRechargeService service) { this.service = service; }

    @PostMapping("/recharges")
    public ApiResponse<WalletRechargeService.RechargeView> recharge(@AuthenticationPrincipal Jwt jwt,
                                                                     @Valid @RequestBody RechargeRequest request) {
        return ApiResponse.success(service.recharge(jwt.getClaimAsString("accountType"),
                Long.parseLong(jwt.getSubject()), request.amountCent(), request.requestId()));
    }

    @GetMapping("/transactions")
    public ApiResponse<List<WalletRechargeService.TransactionView>> transactions(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(service.listTransactions(jwt.getClaimAsString("accountType"), Long.parseLong(jwt.getSubject())));
    }

    public record RechargeRequest(
            @NotNull @Positive @Max(WalletRechargeService.MAX_AMOUNT_CENT) Long amountCent,
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,64}") String requestId) {}
}
