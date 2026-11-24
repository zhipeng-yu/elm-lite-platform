package com.elmlite.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.elmlite.platform.entity.PointsMallRedemption;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface PointsMallRedemptionMapper extends BaseMapper<PointsMallRedemption> {
    @Select("SELECT * FROM points_mall_redemption WHERE user_id=#{userId} AND request_key=#{requestKey} FOR UPDATE")
    PointsMallRedemption lockRequest(@Param("userId") long userId, @Param("requestKey") String requestKey);
    @Select("SELECT * FROM points_mall_redemption WHERE user_id=#{userId} ORDER BY id DESC")
    List<PointsMallRedemption> findForUser(@Param("userId") long userId);
    @Insert("INSERT INTO points_mall_allocation(redemption_id,grant_id,points) VALUES(#{redemptionId},#{grantId},#{points})")
    int allocate(@Param("redemptionId") long redemptionId, @Param("grantId") long grantId, @Param("points") long points);
}
