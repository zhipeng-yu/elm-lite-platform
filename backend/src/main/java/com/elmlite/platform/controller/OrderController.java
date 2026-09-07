package com.elmlite.platform.controller;

import com.elmlite.platform.common.ApiResponse;
import com.elmlite.platform.dto.OrderRequest;
import com.elmlite.platform.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final OrderService orders;

    public OrderController(OrderService orders) { this.orders = orders; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OrderService.Detail> create(@AuthenticationPrincipal Jwt jwt,
                                                  @Valid @RequestBody OrderRequest request) {
        return ApiResponse.success(orders.create(Long.parseLong(jwt.getSubject()), request));
    }

    @GetMapping
    public ApiResponse<List<OrderService.Summary>> list(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(orders.list(Long.parseLong(jwt.getSubject())));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderService.Detail> get(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") long id) {
        return ApiResponse.success(orders.get(Long.parseLong(jwt.getSubject()), id));
    }
}
