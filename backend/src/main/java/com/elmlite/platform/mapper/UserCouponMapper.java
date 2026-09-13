package com.elmlite.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.elmlite.platform.entity.UserCoupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserCouponMapper extends BaseMapper<UserCoupon> {

    @Select("""
            SELECT id, user_id, coupon_id, status, created_at, updated_at
            FROM user_coupon
            WHERE id = #{id}
            FOR UPDATE
            """)
    UserCoupon selectByIdForUpdate(@Param("id") long id);

    @Update("""
            UPDATE user_coupon
            SET status = #{newStatus},
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{id}
              AND user_id = #{userId}
              AND status = #{oldStatus}
            """)
    int updateStatusIfMatches(
            @Param("id") long id,
            @Param("userId") long userId,
            @Param("oldStatus") int oldStatus,
            @Param("newStatus") int newStatus);
}
