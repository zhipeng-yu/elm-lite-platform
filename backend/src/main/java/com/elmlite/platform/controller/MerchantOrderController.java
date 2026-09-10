package com.elmlite.platform.controller;

import com.elmlite.platform.common.ApiResponse;
import com.elmlite.platform.service.MerchantOrderService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/merchant")
public class MerchantOrderController {
    private final MerchantOrderService orders;

    public MerchantOrderController(MerchantOrderService orders) { this.orders = orders; }

    @GetMapping("/shops/{shopId}/orders")
    public ApiResponse<List<MerchantOrderService.Summary>> list(@AuthenticationPrincipal Jwt jwt,
            @PathVariable("shopId") long shopId, @RequestParam(value = "orderStatus", required = false) Integer orderStatus) {
        return ApiResponse.success(orders.list(Long.parseLong(jwt.getSubject()), shopId, orderStatus));
    }

    @GetMapping("/orders/{id}")
    public ApiResponse<MerchantOrderService.Detail> get(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") long id) {
        return ApiResponse.success(orders.get(Long.parseLong(jwt.getSubject()), id));
    }
}
