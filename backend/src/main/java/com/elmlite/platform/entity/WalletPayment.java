package com.elmlite.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("wallet_payment")
public class WalletPayment {
    @TableId(value = "transaction_id", type = IdType.INPUT)
    private Long transactionId;
    private String settlementStatus;
    private LocalDateTime confirmedAt;
    public Long getTransactionId() { return transactionId; }
    public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }
    public String getSettlementStatus() { return settlementStatus; }
    public void setSettlementStatus(String status) { this.settlementStatus = status; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
}
