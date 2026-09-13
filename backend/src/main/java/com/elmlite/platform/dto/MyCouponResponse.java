package com.elmlite.platform.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public record MyCouponResponse(
        Long userCouponId,
        Long couponId,
        Long shopId,
        String name,
        long thresholdCent,
        long discountCent,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime startsAt,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime expiresAt,
        boolean enabled,
        String status) {
}
