package com.elmlite.platform.controller;

import com.elmlite.platform.common.ApiResponse;
import com.elmlite.platform.service.OrderService;
import com.elmlite.platform.service.RiderOrderService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/rider")
public class RiderOrderController {
 private final RiderOrderService orders;
 public RiderOrderController(RiderOrderService orders) { this.orders=orders; }
 private long id(Jwt jwt) { return Long.parseLong(jwt.getSubject()); }
 @GetMapping("/available-orders") public ApiResponse<List<RiderOrderService.Available>> available(@AuthenticationPrincipal Jwt jwt) { return ApiResponse.success(orders.available(id(jwt))); }
 @GetMapping("/orders") public ApiResponse<List<OrderService.Summary>> mine(@AuthenticationPrincipal Jwt jwt) { return ApiResponse.success(orders.mine(id(jwt))); }
 @GetMapping("/orders/{orderId}") public ApiResponse<OrderService.Detail> get(@AuthenticationPrincipal Jwt jwt,@PathVariable long orderId) { return ApiResponse.success(orders.get(id(jwt),orderId)); }
 @PostMapping("/orders/{orderId}/claim") public ApiResponse<OrderService.Detail> claim(@AuthenticationPrincipal Jwt jwt,@PathVariable long orderId) { return ApiResponse.success(orders.claim(id(jwt),orderId)); }
 @PostMapping("/orders/{orderId}/dispatch") public ApiResponse<OrderService.Detail> dispatch(@AuthenticationPrincipal Jwt jwt,@PathVariable long orderId) { return ApiResponse.success(orders.dispatch(id(jwt),orderId)); }
 @PostMapping("/orders/{orderId}/complete") public ApiResponse<OrderService.Detail> complete(@AuthenticationPrincipal Jwt jwt,@PathVariable long orderId) { return ApiResponse.success(orders.complete(id(jwt),orderId)); }
}
