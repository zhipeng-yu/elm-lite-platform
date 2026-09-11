package com.elmlite.platform.service;

import com.elmlite.platform.entity.Coupon;
import com.elmlite.platform.mapper.CouponMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class MerchantCouponService {

    private final CouponMapper couponMapper;

    public MerchantCouponService(CouponMapper couponMapper) {
        this.couponMapper = couponMapper;
    }

    @Transactional
    public Coupon create(
            long merchantId,
            long shopId,
            String name,
            Long thresholdCent,
            Long discountCent,
            LocalDateTime startsAt,
            LocalDateTime expiresAt) {

        Coupon coupon = new Coupon();
        coupon.setShopId(shopId);
        coupon.setName(name);
        coupon.setThresholdAmount(
                BigDecimal.valueOf(thresholdCent, 2));
        coupon.setDiscountAmount(
                BigDecimal.valueOf(discountCent, 2));
        coupon.setStartsAt(startsAt);
        coupon.setExpiresAt(expiresAt);
        coupon.setEnabled(1);

        couponMapper.insert(coupon);

        return coupon;
    }
}