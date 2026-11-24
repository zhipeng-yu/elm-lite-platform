package com.elmlite.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;

@TableName("points_grant")
public class PointsGrant {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String sourceType;
    private String eventKey;
    private Long basisUnits;
    private Long awardedPoints;
    private Long remainingPoints;
    private LocalDate earnedOn;
    private LocalDate expiresOn;

    public Long getId() { return id; }
    public void setId(Long value) { id = value; }
    public Long getUserId() { return userId; }
    public void setUserId(Long value) { userId = value; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String value) { sourceType = value; }
    public String getEventKey() { return eventKey; }
    public void setEventKey(String value) { eventKey = value; }
    public Long getBasisUnits() { return basisUnits; }
    public void setBasisUnits(Long value) { basisUnits = value; }
    public Long getAwardedPoints() { return awardedPoints; }
    public void setAwardedPoints(Long value) { awardedPoints = value; }
    public Long getRemainingPoints() { return remainingPoints; }
    public void setRemainingPoints(Long value) { remainingPoints = value; }
    public LocalDate getEarnedOn() { return earnedOn; }
    public void setEarnedOn(LocalDate value) { earnedOn = value; }
    public LocalDate getExpiresOn() { return expiresOn; }
    public void setExpiresOn(LocalDate value) { expiresOn = value; }
}
