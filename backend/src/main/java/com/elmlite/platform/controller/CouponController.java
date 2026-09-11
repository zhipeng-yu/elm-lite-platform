package com.elmlite.platform.controller;

import com.elmlite.platform.common.ApiResponse;
import com.elmlite.platform.entity.Coupon;
import com.elmlite.platform.service.CouponService;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
public class CouponController {

    private final CouponService couponService;

    public CouponController(
            CouponService couponService) {

        this.couponService = couponService;
    }

    @GetMapping(
            "/api/v1/shops/{shopId}/coupons")
    public ApiResponse<List<CouponResponse>>
            listClaimable(
                    @PathVariable("shopId")
                    long shopId) {

        List<CouponResponse> coupons =
                couponService
                        .listClaimable(shopId)
                        .stream()
                        .map(CouponResponse::from)
                        .toList();

        return ApiResponse.success(coupons);
    }

    @PostMapping(
            "/api/v1/coupons/{id}/claims")
    public ResponseEntity<ApiResponse<Void>>
            claim(
                    @AuthenticationPrincipal
                    Jwt jwt,
                    @PathVariable("id")
                    long id) {

        couponService.claim(
                Long.parseLong(
                        jwt.getSubject()),
                id);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                null));
    }

    public record CouponResponse(
            Long id,
            Long shopId,
            String name,
            Long thresholdCent,
            Long discountCent,
            @JsonFormat(
                    pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime startsAt,
            @JsonFormat(
                    pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime expiresAt,
            Boolean enabled) {

        static CouponResponse from(
                Coupon coupon) {

            return new CouponResponse(
                    coupon.getId(),
                    coupon.getShopId(),
                    coupon.getName(),
                    coupon.getThresholdAmount()
                            .movePointRight(2)
                            .longValueExact(),
                    coupon.getDiscountAmount()
                            .movePointRight(2)
                            .longValueExact(),
                    coupon.getStartsAt(),
                    coupon.getExpiresAt(),
                    Integer.valueOf(1)
                            .equals(
                                    coupon.getEnabled()));
        }
    }
}