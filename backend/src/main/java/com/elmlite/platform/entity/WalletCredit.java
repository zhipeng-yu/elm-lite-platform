package com.elmlite.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;

@TableName("wallet_credit_account")
public class WalletCredit {

    @TableId(value = "wallet_id", type = IdType.INPUT)
    private Long walletId;
    private Long creditLimitCent;
    private Integer repaymentDays;
    private Integer dailyRateBasisPoints;
    private Long principalCent;
    private Long accruedInterestCent;
    private LocalDate dueDate;
    private LocalDate interestCalculatedThrough;

    public Long getWalletId() { return walletId; }
    public void setWalletId(Long value) { this.walletId = value; }

    public Long getCreditLimitCent() { return creditLimitCent; }
    public void setCreditLimitCent(Long value) { this.creditLimitCent = value; }

    public Integer getRepaymentDays() { return repaymentDays; }
    public void setRepaymentDays(Integer value) { this.repaymentDays = value; }

    public Integer getDailyRateBasisPoints() { return dailyRateBasisPoints; }
    public void setDailyRateBasisPoints(Integer value) { this.dailyRateBasisPoints = value; }

    public Long getPrincipalCent() { return principalCent; }
    public void setPrincipalCent(Long value) { this.principalCent = value; }

    public Long getAccruedInterestCent() { return accruedInterestCent; }
    public void setAccruedInterestCent(Long value) { this.accruedInterestCent = value; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate value) { this.dueDate = value; }

    public LocalDate getInterestCalculatedThrough() { return interestCalculatedThrough; }
    public void setInterestCalculatedThrough(LocalDate value) { this.interestCalculatedThrough = value; }
}
