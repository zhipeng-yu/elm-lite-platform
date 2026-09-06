package com.elmlite.platform.controller;

import com.elmlite.platform.common.ApiResponse;
import com.elmlite.platform.service.CartService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cart/items")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ApiResponse<List<CartService.CartItemResponse>> list(
            @AuthenticationPrincipal Jwt jwt) {

        return ApiResponse.success(
                cartService.list(
                        Long.parseLong(jwt.getSubject())));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CartService.CartItemResponse> add(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody AddCartItemRequest request) {

        return ApiResponse.success(
                cartService.add(
                        Long.parseLong(jwt.getSubject()),
                        request.productId(),
                        request.quantity()));
    }

    public record AddCartItemRequest(
            Long productId,
            Integer quantity) {
    }
}
