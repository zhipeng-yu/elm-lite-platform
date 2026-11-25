package com.elmlite.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("wallet_recharge_rule")
public class WalletRechargeRule {
    @TableId(type = IdType.INPUT)
    private Long id;
    private Long thresholdCent;
    private Long rewardCent;
    private Integer enabled;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getThresholdCent() { return thresholdCent; }
    public void setThresholdCent(Long thresholdCent) { this.thresholdCent = thresholdCent; }
    public Long getRewardCent() { return rewardCent; }
    public void setRewardCent(Long rewardCent) { this.rewardCent = rewardCent; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }
}
