package com.elmlite.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.elmlite.platform.entity.WalletTransaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface WalletTransactionMapper extends BaseMapper<WalletTransaction> {
    String FIELDS = "id, transaction_no, transaction_type, request_wallet_id, request_key, "
            + "source_wallet_id, destination_wallet_id, amount_cent, reward_cent, fee_cent, "
            + "source_balance_after_cent, destination_balance_after_cent, created_at";

    @Select("SELECT " + FIELDS + " FROM wallet_transaction WHERE request_wallet_id=#{walletId} AND request_key=#{requestKey}")
    WalletTransaction findByRequest(@Param("walletId") long walletId, @Param("requestKey") String requestKey);

    @Select("SELECT " + FIELDS + " FROM wallet_transaction WHERE source_wallet_id=#{walletId} "
            + "OR destination_wallet_id=#{walletId} ORDER BY id DESC")
    List<WalletTransaction> findByWallet(@Param("walletId") long walletId);
}
