package com.elmlite.platform.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.elmlite.platform.entity.Coupon;
import com.elmlite.platform.entity.UserCoupon;
import com.elmlite.platform.exception.BusinessException;
import com.elmlite.platform.mapper.CouponMapper;
import com.elmlite.platform.mapper.UserCouponMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public UserCoupon claim(
            long userId,
            long couponId) {

        userService.getCurrent(userId);

        Coupon coupon = couponMapper.selectById(couponId);

        if (coupon == null) {
            throw new BusinessException(
                    HttpStatus.NOT_FOUND,
                    "优惠券不存在");
        }

        UserCoupon existing =
                userCouponMapper.selectOne(
                        Wrappers.<UserCoupon>lambdaQuery()
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

        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(couponId);
        userCoupon.setStatus(0);

        try {
            userCouponMapper.insert(userCoupon);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "该优惠券已领取");
        }

        return userCoupon;
    }
}