package com.elmlite.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.elmlite.platform.entity.CartItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CartItemMapper extends BaseMapper<CartItem> {
    @Select("SELECT COALESCE(SUM(quantity), 0) FROM cart_item WHERE product_id = #{productId}")
    long sumQuantityByProductId(@Param("productId") long productId);
}
