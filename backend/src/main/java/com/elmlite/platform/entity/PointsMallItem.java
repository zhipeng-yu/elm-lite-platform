package com.elmlite.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("points_mall_item")
public class PointsMallItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Long pointsCost;
    private Integer stock;
    private Integer enabled;
    public Long getId() { return id; }
    public void setId(Long value) { id = value; }
    public String getName() { return name; }
    public void setName(String value) { name = value; }
    public Long getPointsCost() { return pointsCost; }
    public void setPointsCost(Long value) { pointsCost = value; }
    public Integer getStock() { return stock; }
    public void setStock(Integer value) { stock = value; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer value) { enabled = value; }
}
