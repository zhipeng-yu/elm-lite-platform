package com.elmlite.platform.controller;

import com.elmlite.platform.common.ApiResponse;
import com.elmlite.platform.exception.BusinessException;
import com.elmlite.platform.service.PointsOrderService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
public class PointsOrderController {
    private final PointsOrderService service;
    public PointsOrderController(PointsOrderService service) { this.service = service; }

    @GetMapping("/api/v1/points/me/spends")
    public ApiResponse<List<PointsOrderService.SpendView>> list(@AuthenticationPrincipal Jwt jwt) {
        if (!"USER".equals(jwt.getClaimAsString("accountType"))) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Account type not allowed");
        }
        return ApiResponse.success(service.history(Long.parseLong(jwt.getSubject())));
    }
}
