package com.elmlite.platform.controller;

import com.elmlite.platform.common.ApiResponse;
import com.elmlite.platform.entity.Rider;
import com.elmlite.platform.service.RiderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/riders")
public class RiderController {
    private final RiderService riders;

    public RiderController(RiderService riders) { this.riders = riders; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RiderResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(RiderResponse.from(
                riders.register(request.username(), request.password(), request.displayName())));
    }

    public record RegisterRequest(
            @NotBlank(message = "骑手账号不能为空") @Size(max = 50, message = "骑手账号不能超过50个字符") String username,
            @NotBlank(message = "密码不能为空") @Size(min = 8, max = 72, message = "密码长度必须为8至72位") String password,
            @NotBlank(message = "骑手姓名不能为空") @Size(max = 50, message = "骑手姓名不能超过50个字符") String displayName) {
        @Override
        public String toString() { return "RegisterRequest[REDACTED]"; }
    }

    public record RiderResponse(Long id, String username, String displayName) {
        static RiderResponse from(Rider rider) {
            return new RiderResponse(rider.getId(), rider.getUsername(), rider.getDisplayName());
        }
    }
}
