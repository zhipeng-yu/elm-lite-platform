package com.elmlite.platform.controller;

import com.elmlite.platform.common.ApiResponse;
import com.elmlite.platform.service.WalletAccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wallets")
public class WalletController {
    private final WalletAccountService wallets;

    public WalletController(WalletAccountService wallets) {
        this.wallets = wallets;
    }

    @PostMapping("/me")
    public ResponseEntity<ApiResponse<WalletAccountService.WalletView>> open(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                wallets.open(jwt.getClaimAsString("accountType"), Long.parseLong(jwt.getSubject()))));
    }

    @GetMapping("/me")
    public ApiResponse<WalletAccountService.WalletView> get(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(wallets.get(jwt.getClaimAsString("accountType"), Long.parseLong(jwt.getSubject())));
    }
}
