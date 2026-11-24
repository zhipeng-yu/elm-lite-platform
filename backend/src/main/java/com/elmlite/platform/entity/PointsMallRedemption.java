package com.elmlite.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("points_mall_redemption")
public class PointsMallRedemption {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long itemId;
    private String itemName;
    private Integer quantity;
    private Long pointsPerItem;
    private Long totalPoints;
    private String requestKey;
    private LocalDateTime createdAt;
    public Long getId() { return id; }
    public void setId(Long value) { id = value; }
    public Long getUserId() { return userId; }
    public void setUserId(Long value) { userId = value; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long value) { itemId = value; }
    public String getItemName() { return itemName; }
    public void setItemName(String value) { itemName = value; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer value) { quantity = value; }
    public Long getPointsPerItem() { return pointsPerItem; }
    public void setPointsPerItem(Long value) { pointsPerItem = value; }
    public Long getTotalPoints() { return totalPoints; }
    public void setTotalPoints(Long value) { totalPoints = value; }
    public String getRequestKey() { return requestKey; }
    public void setRequestKey(String value) { requestKey = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { createdAt = value; }
}
