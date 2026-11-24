package com.elmlite.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.elmlite.platform.entity.PointsRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface PointsRuleMapper extends BaseMapper<PointsRule> {

    @Select("SELECT source_type, units_per_award, points_per_award, valid_days, enabled FROM points_rule ORDER BY source_type")
    List<PointsRule> findAll();
}
