package com.elmlite.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.elmlite.platform.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    @Update("""
            UPDATE product
            SET stock = stock - #{quantity}
            WHERE id = #{productId}
              AND status = 1
              AND stock >= #{quantity}
            """)
    int deductStock(
            @Param("productId") long productId,
            @Param("quantity") int quantity);
}
