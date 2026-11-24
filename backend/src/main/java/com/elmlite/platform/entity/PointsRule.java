package com.elmlite.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("points_rule")
public class PointsRule {

    @TableId(value = "source_type", type = IdType.INPUT)
    private String sourceType;
    private Long unitsPerAward;
    private Long pointsPerAward;
    private Integer validDays;
    private Integer enabled;

    public String getSourceType() { return sourceType; }
    public void setSourceType(String value) { this.sourceType = value; }

    public Long getUnitsPerAward() { return unitsPerAward; }
    public void setUnitsPerAward(Long value) { this.unitsPerAward = value; }

    public Long getPointsPerAward() { return pointsPerAward; }
    public void setPointsPerAward(Long value) { this.pointsPerAward = value; }

    public Integer getValidDays() { return validDays; }
    public void setValidDays(Integer value) { this.validDays = value; }

    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer value) { this.enabled = value; }
}
