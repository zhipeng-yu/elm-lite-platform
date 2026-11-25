package com.elmlite.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.elmlite.platform.entity.WalletPayment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface WalletPaymentMapper extends BaseMapper<WalletPayment> {
    @Select("SELECT transaction_id, settlement_status, confirmed_at FROM wallet_payment WHERE transaction_id = #{id} FOR UPDATE")
    WalletPayment lockById(@Param("id") long id);
}
