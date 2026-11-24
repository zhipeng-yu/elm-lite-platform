package com.elmlite.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.elmlite.platform.entity.PointsGrant;
import com.elmlite.platform.entity.PointsOrderAllocation;
import com.elmlite.platform.entity.PointsOrderSpend;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface PointsOrderSpendMapper extends BaseMapper<PointsOrderSpend> {
    @Select("SELECT * FROM points_grant WHERE user_id = #{userId} AND remaining_points > 0 "
            + "AND expires_on > #{today} ORDER BY expires_on, id FOR UPDATE")
    List<PointsGrant> lockAvailable(@Param("userId") long userId, @Param("today") LocalDate today);

    @Update("UPDATE points_grant SET remaining_points = remaining_points - #{points} "
            + "WHERE id = #{grantId} AND user_id = #{userId} AND remaining_points >= #{points}")
    int deduct(@Param("grantId") long grantId, @Param("userId") long userId, @Param("points") long points);

    @Insert("INSERT INTO points_order_allocation (spend_id, grant_id, points) "
            + "VALUES (#{spendId}, #{grantId}, #{points})")
    int allocate(@Param("spendId") long spendId, @Param("grantId") long grantId, @Param("points") long points);

    @Select("SELECT * FROM points_order_spend WHERE order_id = #{orderId} FOR UPDATE")
    PointsOrderSpend lockForOrder(@Param("orderId") long orderId);

    @Select("SELECT grant_id, points FROM points_order_allocation WHERE spend_id = #{spendId} ORDER BY grant_id")
    List<PointsOrderAllocation> allocations(@Param("spendId") long spendId);

    @Update("UPDATE points_grant SET remaining_points = remaining_points + #{points} "
            + "WHERE id = #{grantId} AND user_id = #{userId} AND remaining_points <= awarded_points - #{points}")
    int restore(@Param("grantId") long grantId, @Param("userId") long userId, @Param("points") long points);

    @Update("UPDATE points_order_spend SET status = 'RETURNED' WHERE id = #{id} AND status = 'SPENT'")
    int markReturned(@Param("id") long id);

    @Select("SELECT * FROM points_order_spend WHERE user_id = #{userId} ORDER BY id DESC")
    List<PointsOrderSpend> findForUser(@Param("userId") long userId);
}
