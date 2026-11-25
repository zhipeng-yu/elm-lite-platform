package com.elmlite.platform.service;

import com.elmlite.platform.entity.Merchant;
import com.elmlite.platform.entity.WalletEntity;
import com.elmlite.platform.exception.BusinessException;
import com.elmlite.platform.mapper.MerchantMapper;
import com.elmlite.platform.mapper.WalletMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletAccountService {
    private final WalletMapper wallets;
    private final UserService users;
    private final MerchantMapper merchants;

    public WalletAccountService(WalletMapper wallets, UserService users, MerchantMapper merchants) {
        this.wallets = wallets;
        this.users = users;
        this.merchants = merchants;
    }

    @Transactional
    public WalletView open(String ownerType, long ownerId) {
        requireActiveOwner(ownerType, ownerId);
        WalletEntity wallet = new WalletEntity();
        wallet.setOwnerType(ownerType);
        wallet.setOwnerId(ownerId);
        wallet.setBalanceCent(0L);
        wallet.setFrozenBalanceCent(0L);
        try {
            wallets.insert(wallet);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(HttpStatus.CONFLICT, "钱包已存在");
        }
        return WalletView.from(wallet);
    }

    @Transactional(readOnly = true)
    public WalletView get(String ownerType, long ownerId) {
        requireActiveOwner(ownerType, ownerId);
        WalletEntity wallet = wallets.findByOwner(ownerType, ownerId);
        if (wallet == null) throw new BusinessException(HttpStatus.NOT_FOUND, "钱包不存在");
        return WalletView.from(wallet);
    }

    private void requireActiveOwner(String ownerType, long ownerId) {
        if ("USER".equals(ownerType)) {
            users.getCurrent(ownerId);
        } else if ("MERCHANT".equals(ownerType)) {
            Merchant merchant = merchants.selectById(ownerId);
            if (merchant == null) throw new BusinessException(HttpStatus.NOT_FOUND, "商家不存在");
            if (!Integer.valueOf(1).equals(merchant.getStatus())) {
                throw new BusinessException(HttpStatus.FORBIDDEN, "商家账号不可用");
            }
        } else {
            throw new BusinessException(HttpStatus.FORBIDDEN, "无权访问钱包");
        }
    }

    public record WalletView(long id, long balanceCent, long frozenBalanceCent, long availableBalanceCent) {
        static WalletView from(WalletEntity wallet) {
            long balance = wallet.getBalanceCent();
            long frozen = wallet.getFrozenBalanceCent();
            return new WalletView(wallet.getId(), balance, frozen, Math.subtractExact(balance, frozen));
        }
    }
}
