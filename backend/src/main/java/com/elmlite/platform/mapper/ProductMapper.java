package com.elmlite.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.elmlite.platform.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
    @Select("SELECT * FROM product WHERE id = #{id} FOR UPDATE")
    Product lockById(@Param("id") long id);

    @Update("""
            UPDATE product SET stock = stock - #{quantity}, updated_at = CURRENT_TIMESTAMP
            WHERE id = #{id} AND status = 1 AND stock >= #{quantity} AND #{quantity} > 0
            """)
    int deductStock(@Param("id") long id, @Param("quantity") int quantity);
}
