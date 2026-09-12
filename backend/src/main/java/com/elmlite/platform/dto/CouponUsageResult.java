package com.elmlite.platform.dto;

public record CouponUsageResult(
        long userCouponId,
        long couponId,
        long discountAmountCent) {
}
