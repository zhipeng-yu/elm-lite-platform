package com.elmlite.platform.controller;

import com.elmlite.platform.common.ApiResponse;
import com.elmlite.platform.service.WalletPaymentService;
import com.elmlite.platform.service.WalletRechargeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/wallets/me/payments")
public class WalletPaymentController {
    private final WalletPaymentService service;
    public WalletPaymentController(WalletPaymentService service) { this.service = service; }

    @PostMapping
    public ApiResponse<WalletPaymentService.PaymentView> pay(@AuthenticationPrincipal Jwt jwt,
                                                              @Valid @RequestBody PaymentRequest request) {
        return ApiResponse.success(service.pay(jwt.getClaimAsString("accountType"), Long.parseLong(jwt.getSubject()),
                request.recipientType(), request.recipientId(), request.amountCent(),
                request.requestId(), Boolean.TRUE.equals(request.useCredit())));
    }
    @GetMapping("/{id}")
    public ApiResponse<WalletPaymentService.PaymentView> get(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return ApiResponse.success(service.get(jwt.getClaimAsString("accountType"), Long.parseLong(jwt.getSubject()), id));
    }
    @PostMapping("/{id}/confirm")
    public ApiResponse<WalletPaymentService.PaymentView> confirm(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return ApiResponse.success(service.confirm(jwt.getClaimAsString("accountType"), Long.parseLong(jwt.getSubject()), id));
    }
    public record PaymentRequest(@NotBlank @Pattern(regexp="USER|MERCHANT") String recipientType,
                                 @NotNull @Positive Long recipientId,
                                 @NotNull @Positive @Max(WalletRechargeService.MAX_AMOUNT_CENT) Long amountCent,
                                 @NotBlank @Pattern(regexp="[A-Za-z0-9_-]{1,64}") String requestId, Boolean useCredit) {}
}
