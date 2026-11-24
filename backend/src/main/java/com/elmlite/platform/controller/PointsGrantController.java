package com.elmlite.platform.controller;

import com.elmlite.platform.common.ApiResponse;
import com.elmlite.platform.exception.BusinessException;
import com.elmlite.platform.service.PointsGrantService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
public class PointsGrantController {
    private final PointsGrantService service;
    public PointsGrantController(PointsGrantService service) { this.service = service; }

    @PostMapping("/api/v1/admin/points/grants")
    public ApiResponse<PointsGrantService.GrantView> grant(@AuthenticationPrincipal Jwt jwt,
                                                           @Valid @RequestBody GrantRequest request) {
        requireRole(jwt, "ADMIN");
        return ApiResponse.success(service.grant(Long.parseLong(jwt.getSubject()), request.userId(),
                request.sourceType(), request.eventKey(), request.basisUnits()));
    }

    @GetMapping("/api/v1/points/me")
    public ApiResponse<PointsGrantService.BalanceView> balance(@AuthenticationPrincipal Jwt jwt) {
        requireRole(jwt, "USER");
        return ApiResponse.success(service.balance(Long.parseLong(jwt.getSubject())));
    }

    @GetMapping("/api/v1/points/me/grants")
    public ApiResponse<List<PointsGrantService.GrantView>> history(@AuthenticationPrincipal Jwt jwt) {
        requireRole(jwt, "USER");
        return ApiResponse.success(service.history(Long.parseLong(jwt.getSubject())));
    }

    private static void requireRole(Jwt jwt, String required) {
        if (!required.equals(jwt.getClaimAsString("accountType"))) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Account type not allowed");
        }
    }

    public record GrantRequest(
            @NotNull @Positive Long userId,
            @NotBlank @Pattern(regexp = "ORDER|REVIEW|ACTIVITY") String sourceType,
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_.:-]{1,64}") String eventKey,
            @NotNull @PositiveOrZero @Max(9_999_999_999L) Long basisUnits) { }
}
