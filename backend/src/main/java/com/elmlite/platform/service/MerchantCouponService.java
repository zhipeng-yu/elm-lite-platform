package com.elmlite.platform.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.elmlite.platform.entity.Coupon;
import com.elmlite.platform.entity.Merchant;
import com.elmlite.platform.entity.Shop;
import com.elmlite.platform.exception.BusinessException;
import com.elmlite.platform.mapper.CouponMapper;
import com.elmlite.platform.mapper.MerchantMapper;
import com.elmlite.platform.mapper.ShopMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MerchantCouponService {

    private final CouponMapper couponMapper;
    private final MerchantMapper merchantMapper;
    private final ShopMapper shopMapper;

    public MerchantCouponService(
            CouponMapper couponMapper,
            MerchantMapper merchantMapper,
            ShopMapper shopMapper) {

        this.couponMapper = couponMapper;
        this.merchantMapper = merchantMapper;
        this.shopMapper = shopMapper;
    }

    public List<Coupon> list(
            long merchantId,
            long shopId) {

        requireActiveMerchant(merchantId);
        requireOwnedShop(merchantId, shopId);

        return couponMapper.selectList(
                Wrappers.<Coupon>lambdaQuery()
                        .eq(Coupon::getShopId, shopId)
                        .orderByAsc(Coupon::getId));
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

        requireActiveMerchant(merchantId);
        requireOwnedShop(merchantId, shopId);

        validateName(name);
        validateThreshold(thresholdCent);
        validateDiscount(discountCent);
        validateTimeRange(startsAt, expiresAt);

        Coupon coupon = new Coupon();
        coupon.setShopId(shopId);
        coupon.setName(name.trim());
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

    @Transactional
    public Coupon updateEnabled(
            long merchantId,
            long couponId,
            Boolean enabled) {

        requireActiveMerchant(merchantId);

        Coupon current =
                couponMapper.selectById(couponId);

        if (current == null) {
            throw new BusinessException(
                    HttpStatus.NOT_FOUND,
                    "优惠券不存在");
        }

        requireOwnedShop(
                merchantId,
                current.getShopId());

        if (enabled == null) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "优惠券状态不能为空");
        }

        Coupon coupon = new Coupon();
        coupon.setId(couponId);
        coupon.setEnabled(enabled ? 1 : 0);
        coupon.setUpdatedAt(LocalDateTime.now());

        couponMapper.updateById(coupon);

        return couponMapper.selectById(couponId);
    }

    private void requireActiveMerchant(
            long merchantId) {

        Merchant merchant =
                merchantMapper.selectById(merchantId);

        if (merchant == null
                || !Integer.valueOf(1)
                .equals(merchant.getStatus())) {

            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "商家账号不可用");
        }
    }

    private Shop requireOwnedShop(
            long merchantId,
            long shopId) {

        Shop shop = shopMapper.selectById(shopId);

        if (shop == null) {
            throw new BusinessException(
                    HttpStatus.NOT_FOUND,
                    "店铺不存在");
        }

        if (!Long.valueOf(merchantId)
                .equals(shop.getMerchantId())) {

            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "无权操作该店铺");
        }

        return shop;
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "优惠券名称不能为空");
        }
    }

    private void validateThreshold(
            Long thresholdCent) {

        if (thresholdCent == null
                || thresholdCent < 0) {

            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "优惠券门槛不能为负数");
        }
    }

    private void validateDiscount(
            Long discountCent) {

        if (discountCent == null
                || discountCent <= 0) {

            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "优惠金额必须大于0");
        }
    }

    private void validateTimeRange(
            LocalDateTime startsAt,
            LocalDateTime expiresAt) {

        if (startsAt == null
                || expiresAt == null
                || !startsAt.isBefore(expiresAt)) {

            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "优惠券开始时间必须早于到期时间");
        }
    }
}