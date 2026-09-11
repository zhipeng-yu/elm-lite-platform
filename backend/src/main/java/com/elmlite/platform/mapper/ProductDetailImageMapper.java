package com.elmlite.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.elmlite.platform.entity.ProductDetailImage;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProductDetailImageMapper
        extends BaseMapper<ProductDetailImage> {

    @Select("""
            SELECT *
            FROM product_detail_image
            WHERE product_id = #{productId}
            ORDER BY sort_order ASC, id ASC
            """)
    List<ProductDetailImage> selectByProductId(
            @Param("productId") long productId);

    @Delete("""
            DELETE FROM product_detail_image
            WHERE product_id = #{productId}
            """)
    int deleteByProductId(
            @Param("productId") long productId);
}
