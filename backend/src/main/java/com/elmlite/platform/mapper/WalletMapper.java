package com.elmlite.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.elmlite.platform.entity.WalletEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface WalletMapper extends BaseMapper<WalletEntity> {
    @Select("SELECT id, owner_type, owner_id, balance_cent, frozen_balance_cent "
            + "FROM virtual_wallet WHERE owner_type = #{ownerType} AND owner_id = #{ownerId}")
    WalletEntity findByOwner(@Param("ownerType") String ownerType, @Param("ownerId") long ownerId);

    @Select("SELECT id, owner_type, owner_id, balance_cent, frozen_balance_cent "
            + "FROM virtual_wallet WHERE owner_type = #{ownerType} AND owner_id = #{ownerId} FOR UPDATE")
    WalletEntity lockByOwner(@Param("ownerType") String ownerType, @Param("ownerId") long ownerId);

    @Select("SELECT id, owner_type, owner_id, balance_cent, frozen_balance_cent "
            + "FROM virtual_wallet WHERE id = #{id} FOR UPDATE")
    WalletEntity lockById(@Param("id") long id);
}
