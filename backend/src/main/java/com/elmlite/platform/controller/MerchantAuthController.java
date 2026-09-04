package com.elmlite.platform.controller;

import com.elmlite.platform.common.ApiResponse;
import com.elmlite.platform.entity.Merchant;
import com.elmlite.platform.service.JwtTokenService;
import com.elmlite.platform.service.MerchantAuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/merchant/auth")
public class MerchantAuthController {

    private final MerchantAuthService merchantAuthService;

    public MerchantAuthController(MerchantAuthService merchantAuthService) {
        this.merchantAuthService = merchantAuthService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {
        MerchantAuthService.LoginResult result =
                merchantAuthService.login(request.account(), request.password());
        Merchant merchant = result.merchant();

        return ApiResponse.success(new LoginResponse(
                result.accessToken(),
                JwtTokenService.EXPIRES_IN_SECONDS,
                new MerchantController.MerchantResponse(
                        merchant.getId(),
                        merchant.getAccount(),
                        merchant.getMerchantName())));
    }

    public record LoginRequest(
            @NotBlank(message = "商家账号不能为空") String account,
            @NotBlank(message = "密码不能为空") String password) {
        @Override
        public String toString() {
            return "LoginRequest[REDACTED]";
        }
    }

    public record LoginResponse(
            String accessToken,
            long expiresIn,
            MerchantController.MerchantResponse merchant) {
        @Override
        public String toString() {
            return "LoginResponse[REDACTED]";
        }
    }
}
