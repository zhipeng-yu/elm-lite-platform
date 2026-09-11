package com.elmlite.platform.controller;

import com.elmlite.platform.common.ApiResponse;
import com.elmlite.platform.service.CouponService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CouponController {

    private final CouponService couponService;

    public CouponController(
            CouponService couponService) {
        this.couponService = couponService;
    }

    @PostMapping("/api/v1/coupons/{id}/claims")
    public ResponseEntity<ApiResponse<Void>> claim(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") long id) {

        couponService.claim(
                Long.parseLong(jwt.getSubject()),
                id);

        ApiResponse<Void> response =
                ApiResponse.success(null);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}