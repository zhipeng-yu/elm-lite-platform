package com.elmlite.platform.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.elmlite.platform.dto.MyCouponResponse;
import com.elmlite.platform.entity.Coupon;
import com.elmlite.platform.entity.UserCoupon;
import com.elmlite.platform.exception.BusinessException;
import com.elmlite.platform.mapper.CouponMapper;
import com.elmlite.platform.mapper.UserCouponMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CouponService {

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;
    private final UserService userService;

    public CouponService(
            CouponMapper couponMapper,
            UserCouponMapper userCouponMapper,
            UserService userService) {

        this.couponMapper = couponMapper;
        this.userCouponMapper = userCouponMapper;
        this.userService = userService;
    }

    public List<Coupon> listClaimable(
            long shopId) {

        LocalDateTime now =
                LocalDateTime.now();

        return couponMapper.selectList(
                Wrappers.<Coupon>lambdaQuery()
                        .eq(
                                Coupon::getShopId,
                                shopId)
                        .eq(
                                Coupon::getEnabled,
                                1)
                        .le(
                                Coupon::getStartsAt,
                                now)
                        .gt(
                                Coupon::getExpiresAt,
                                now)
                        .orderByAsc(
                                Coupon::getId));
    }

    @Transactional
    public UserCoupon claim(
            long userId,
            long couponId) {

        userService.getCurrent(userId);

        Coupon coupon =
                couponMapper.selectById(couponId);

        if (coupon == null) {
            throw new BusinessException(
                    HttpStatus.NOT_FOUND,
                    "优惠券不存在");
        }

        validateClaimable(coupon);

        UserCoupon existing =
                userCouponMapper.selectOne(
                        Wrappers
                                .<UserCoupon>lambdaQuery()
                                .eq(
                                        UserCoupon::getUserId,
                                        userId)
                                .eq(
                                        UserCoupon::getCouponId,
                                        couponId));

        if (existing != null) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "该优惠券已领取");
        }

        UserCoupon userCoupon =
                new UserCoupon();

        userCoupon.setUserId(userId);
        userCoupon.setCouponId(couponId);
        userCoupon.setStatus(0);

        try {
            userCouponMapper.insert(
                    userCoupon);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "该优惠券已领取");
        }

        return userCoupon;
    }

    @Transactional(readOnly = true)
    public List<MyCouponResponse> listMine(long userId) {
        userService.getCurrent(userId);
        LocalDateTime now = LocalDateTime.now();

        return userCouponMapper.selectList(
                        Wrappers.<UserCoupon>lambdaQuery()
                                .eq(UserCoupon::getUserId, userId)
                                .orderByDesc(UserCoupon::getId))
                .stream()
                .map(userCoupon -> toMyCouponResponse(userCoupon, now))
                .toList();
    }

    private MyCouponResponse toMyCouponResponse(
            UserCoupon userCoupon, LocalDateTime now) {
        Coupon coupon = couponMapper.selectById(userCoupon.getCouponId());

        if (coupon == null) {
            throw new BusinessException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "优惠券关联数据异常");
        }

        String displayStatus;
        if (Integer.valueOf(1).equals(userCoupon.getStatus())) {
            displayStatus = "USED";
        } else if (!now.isBefore(coupon.getExpiresAt())) {
            displayStatus = "EXPIRED";
        } else if (!Integer.valueOf(1).equals(coupon.getEnabled())) {
            displayStatus = "DISABLED";
        } else {
            displayStatus = "AVAILABLE";
        }

        return new MyCouponResponse(
                userCoupon.getId(),
                coupon.getId(),
                coupon.getShopId(),
                coupon.getName(),
                coupon.getThresholdAmount().movePointRight(2).longValueExact(),
                coupon.getDiscountAmount().movePointRight(2).longValueExact(),
                coupon.getStartsAt(),
                coupon.getExpiresAt(),
                Integer.valueOf(1).equals(coupon.getEnabled()),
                displayStatus);
    }

    @Transactional(propagation =
            org.springframework.transaction.annotation.Propagation.MANDATORY)
    public com.elmlite.platform.dto.CouponUsageResult consumeCoupon(
            long userId,
            long userCouponId,
            long shopId,
            long productAmountCent) {

        if (productAmountCent < 0) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "商品金额不能为负数");
        }

        UserCoupon userCoupon =
                userCouponMapper.selectByIdForUpdate(userCouponId);

        if (userCoupon == null) {
            throw new BusinessException(
                    HttpStatus.NOT_FOUND,
                    "用户优惠券不存在");
        }

        if (!Long.valueOf(userId).equals(userCoupon.getUserId())) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "无权使用该优惠券");
        }

        if (!Integer.valueOf(0).equals(userCoupon.getStatus())) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "优惠券已使用");
        }

        Coupon coupon = couponMapper.selectById(userCoupon.getCouponId());

        if (coupon == null) {
            throw new BusinessException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "优惠券关联数据异常");
        }

        if (!Long.valueOf(shopId).equals(coupon.getShopId())) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "优惠券不属于当前店铺");
        }

        LocalDateTime now = LocalDateTime.now();

        if (!Integer.valueOf(1).equals(coupon.getEnabled())) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "优惠券已停用");
        }

        if (coupon.getStartsAt() == null
                || now.isBefore(coupon.getStartsAt())) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "优惠券尚未开始");
        }

        if (coupon.getExpiresAt() == null
                || !now.isBefore(coupon.getExpiresAt())) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "优惠券已过期");
        }

        long thresholdCent = coupon.getThresholdAmount()
                .movePointRight(2)
                .longValueExact();

        if (productAmountCent < thresholdCent) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "商品金额未达到优惠券门槛");
        }

        long couponDiscountCent = coupon.getDiscountAmount()
                .movePointRight(2)
                .longValueExact();

        long discountAmountCent =
                Math.min(couponDiscountCent, productAmountCent);

        int changed = userCouponMapper.updateStatusIfMatches(
                userCouponId, userId, 0, 1);

        if (changed != 1) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "优惠券状态已发生变化");
        }

        return new com.elmlite.platform.dto.CouponUsageResult(
                userCouponId,
                coupon.getId(),
                discountAmountCent);
    }

    @Transactional(propagation =
            org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void returnCoupon(long userId, long userCouponId) {
        UserCoupon userCoupon =
                userCouponMapper.selectByIdForUpdate(userCouponId);

        if (userCoupon == null) {
            throw new BusinessException(
                    HttpStatus.NOT_FOUND,
                    "用户优惠券不存在");
        }

        if (!Long.valueOf(userId).equals(userCoupon.getUserId())) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "无权返还该优惠券");
        }

        if (!Integer.valueOf(1).equals(userCoupon.getStatus())) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "优惠券状态不允许返还");
        }

        int changed = userCouponMapper.updateStatusIfMatches(
                userCouponId, userId, 1, 0);

        if (changed != 1) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "优惠券状态已发生变化");
        }
    }

    private void validateClaimable(
            Coupon coupon) {

        LocalDateTime now =
                LocalDateTime.now();

        if (!Integer.valueOf(1)
                .equals(coupon.getEnabled())) {

            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "优惠券已停用");
        }

        if (coupon.getStartsAt() == null
                || now.isBefore(
                        coupon.getStartsAt())) {

            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "优惠券尚未开始");
        }

        if (coupon.getExpiresAt() == null
                || !now.isBefore(
                        coupon.getExpiresAt())) {

            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "优惠券已过期");
        }
    }
}