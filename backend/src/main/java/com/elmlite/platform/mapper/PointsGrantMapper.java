package com.elmlite.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.elmlite.platform.entity.PointsGrant;
import com.elmlite.platform.entity.PointsRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface PointsGrantMapper extends BaseMapper<PointsGrant> {
    @Select("SELECT status FROM users WHERE id = #{userId} FOR UPDATE")
    Integer lockUserStatus(@Param("userId") long userId);

    @Select("SELECT source_type, units_per_award, points_per_award, valid_days, enabled "
            + "FROM points_rule WHERE source_type = #{source} FOR UPDATE")
    PointsRule findRuleForUpdate(@Param("source") String source);

    @Select("SELECT * FROM points_grant WHERE user_id = #{userId} AND source_type = #{source} "
            + "AND event_key = #{eventKey} FOR UPDATE")
    PointsGrant findEventForUpdate(@Param("userId") long userId,
                                   @Param("source") String source,
                                   @Param("eventKey") String eventKey);

    @Select("SELECT * FROM points_grant WHERE user_id = #{userId} ORDER BY id DESC")
    List<PointsGrant> findForUser(@Param("userId") long userId);
}
