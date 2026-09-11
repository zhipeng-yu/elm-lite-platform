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
}
