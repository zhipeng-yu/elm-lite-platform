package com.elmlite.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("wallet_transaction")
public class WalletTransaction {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String transactionNo;
    private String transactionType;
    private Long requestWalletId;
    private String requestKey;
    private Long sourceWalletId;
    private Long destinationWalletId;
    private Long amountCent;
    private Long rewardCent;
    private Long feeCent;
    private Long sourceBalanceAfterCent;
    private Long destinationBalanceAfterCent;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTransactionNo() { return transactionNo; }
    public void setTransactionNo(String transactionNo) { this.transactionNo = transactionNo; }
    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
    public Long getRequestWalletId() { return requestWalletId; }
    public void setRequestWalletId(Long requestWalletId) { this.requestWalletId = requestWalletId; }
    public String getRequestKey() { return requestKey; }
    public void setRequestKey(String requestKey) { this.requestKey = requestKey; }
    public Long getSourceWalletId() { return sourceWalletId; }
    public void setSourceWalletId(Long sourceWalletId) { this.sourceWalletId = sourceWalletId; }
    public Long getDestinationWalletId() { return destinationWalletId; }
    public void setDestinationWalletId(Long destinationWalletId) { this.destinationWalletId = destinationWalletId; }
    public Long getAmountCent() { return amountCent; }
    public void setAmountCent(Long amountCent) { this.amountCent = amountCent; }
    public Long getRewardCent() { return rewardCent; }
    public void setRewardCent(Long rewardCent) { this.rewardCent = rewardCent; }
    public Long getFeeCent() { return feeCent; }
    public void setFeeCent(Long feeCent) { this.feeCent = feeCent; }
    public Long getSourceBalanceAfterCent() { return sourceBalanceAfterCent; }
    public void setSourceBalanceAfterCent(Long value) { this.sourceBalanceAfterCent = value; }
    public Long getDestinationBalanceAfterCent() { return destinationBalanceAfterCent; }
    public void setDestinationBalanceAfterCent(Long value) { this.destinationBalanceAfterCent = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
