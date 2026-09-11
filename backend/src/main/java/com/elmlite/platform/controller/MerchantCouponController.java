package com.elmlite.platform.controller;

import com.elmlite.platform.common.ApiResponse;
import com.elmlite.platform.entity.Coupon;
import com.elmlite.platform.service.MerchantCouponService;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
public class MerchantCouponController {

    private final MerchantCouponService merchantCouponService;

    public MerchantCouponController(
            MerchantCouponService merchantCouponService) {
        this.merchantCouponService = merchantCouponService;
    }

    @PostMapping("/api/v1/merchant/shops/{shopId}/coupons")
    public ResponseEntity<ApiResponse<CouponResponse>> create(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("shopId") long shopId,
            @RequestBody CreateCouponRequest request) {

        Coupon coupon =
                merchantCouponService.create(
                        Long.parseLong(jwt.getSubject()),
                        shopId,
                        request.name(),
                        request.thresholdCent(),
                        request.discountCent(),
                        request.startsAt(),
                        request.expiresAt());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        CouponResponse.from(coupon)));
    }

    @PatchMapping("/api/v1/merchant/coupons/{id}")
    public ApiResponse<CouponResponse> updateEnabled(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") long id,
            @RequestBody UpdateCouponRequest request) {

        Coupon coupon =
                merchantCouponService.updateEnabled(
                        Long.parseLong(jwt.getSubject()),
                        id,
                        request.enabled());

        return ApiResponse.success(
                CouponResponse.from(coupon));
    }

    public record CreateCouponRequest(
            String name,
            Long thresholdCent,
            Long discountCent,
            LocalDateTime startsAt,
            LocalDateTime expiresAt) {
    }

    public record UpdateCouponRequest(
            Boolean enabled) {
    }

    public record CouponResponse(
            Long id,
            Long shopId,
            String name,
            Long thresholdCent,
            Long discountCent,

            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime startsAt,

            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime expiresAt,

            Boolean enabled) {

        static CouponResponse from(Coupon coupon) {
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
                            .equals(coupon.getEnabled()));
        }
    }
}