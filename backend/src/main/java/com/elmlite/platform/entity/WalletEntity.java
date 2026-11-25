package com.elmlite.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("virtual_wallet")
public class WalletEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("owner_type")
    private String ownerType;
    @TableField("owner_id")
    private Long ownerId;
    @TableField("balance_cent")
    private Long balanceCent;
    @TableField("frozen_balance_cent")
    private Long frozenBalanceCent;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOwnerType() { return ownerType; }
    public void setOwnerType(String ownerType) { this.ownerType = ownerType; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public Long getBalanceCent() { return balanceCent; }
    public void setBalanceCent(Long balanceCent) { this.balanceCent = balanceCent; }
    public Long getFrozenBalanceCent() { return frozenBalanceCent; }
    public void setFrozenBalanceCent(Long frozenBalanceCent) { this.frozenBalanceCent = frozenBalanceCent; }
}
