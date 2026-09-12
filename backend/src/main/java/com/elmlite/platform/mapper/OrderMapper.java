package com.elmlite.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.elmlite.platform.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
    @Select("SELECT * FROM orders WHERE id = #{id} FOR UPDATE")
    Order lockById(@Param("id") long id);

    @Update("UPDATE orders SET order_status = #{next}, updated_at = CURRENT_TIMESTAMP " +
            "WHERE id = #{id} AND order_status = #{expected}")
    int updateStatus(@Param("id") long id, @Param("expected") int expected, @Param("next") int next);

    @Update("UPDATE orders SET rider_id = #{riderId}, updated_at = CURRENT_TIMESTAMP " +
            "WHERE id = #{id} AND rider_id IS NULL AND order_status = 2")
    int claim(@Param("id") long id, @Param("riderId") long riderId);
}
