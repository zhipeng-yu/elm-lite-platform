package com.elmlite.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("wallet_withdrawal_rule")
public class WalletWithdrawalRule {

    @TableId(type = IdType.INPUT)
    private Long id;
    private Integer feeBasisPoints;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getFeeBasisPoints() {
        return feeBasisPoints;
    }

    public void setFeeBasisPoints(Integer feeBasisPoints) {
        this.feeBasisPoints = feeBasisPoints;
    }
}
