package com.elmlite.platform.controller;

import com.elmlite.platform.common.ApiResponse;
import com.elmlite.platform.service.JwtTokenService;
import com.elmlite.platform.service.RiderAuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rider/auth")
public class RiderAuthController {
    private final RiderAuthService auth;

    public RiderAuthController(RiderAuthService auth) { this.auth = auth; }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        var result = auth.login(request.username(), request.password());
        return ApiResponse.success(new LoginResponse(result.accessToken(), JwtTokenService.EXPIRES_IN_SECONDS,
                RiderController.RiderResponse.from(result.rider())));
    }

    public record LoginRequest(
            @NotBlank(message = "骑手账号不能为空") String username,
            @NotBlank(message = "密码不能为空") String password) {
        @Override
        public String toString() { return "LoginRequest[REDACTED]"; }
    }

    public record LoginResponse(String accessToken, long expiresIn, RiderController.RiderResponse rider) {
        @Override
        public String toString() { return "LoginResponse[REDACTED]"; }
    }
}
