package com.elmlite.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.elmlite.platform.entity.WalletCredit;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface WalletCreditMapper extends BaseMapper<WalletCredit> {

    @Update("UPDATE wallet_credit_account SET "
            + "principal_cent=#{principalCent}, "
            + "accrued_interest_cent=#{accruedInterestCent}, "
            + "due_date=#{dueDate,jdbcType=DATE}, "
            + "interest_calculated_through=#{interestCalculatedThrough,jdbcType=DATE} "
            + "WHERE wallet_id=#{walletId}")
    int updateDebt(WalletCredit credit);
}
