package com.elmlite.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.elmlite.platform.entity.PointsMallItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

@Mapper
public interface PointsMallItemMapper extends BaseMapper<PointsMallItem> {
    @Select("SELECT * FROM points_mall_item ORDER BY id ASC")
    List<PointsMallItem> findAll();
    @Select("SELECT * FROM points_mall_item WHERE enabled=1 ORDER BY id ASC")
    List<PointsMallItem> findEnabled();
    @Select("SELECT * FROM points_mall_item WHERE id=#{id} FOR UPDATE")
    PointsMallItem lockById(@Param("id") long id);
    @Update("UPDATE points_mall_item SET stock=stock-#{quantity} WHERE id=#{id} AND enabled=1 AND stock>=#{quantity}")
    int deductStock(@Param("id") long id, @Param("quantity") int quantity);
}
