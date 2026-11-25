package com.elmlite.platform.service;

import com.elmlite.platform.entity.Admin;
import com.elmlite.platform.entity.WalletEntity;
import com.elmlite.platform.entity.WalletRechargeRule;
import com.elmlite.platform.entity.WalletTransaction;
import com.elmlite.platform.exception.BusinessException;
import com.elmlite.platform.mapper.AdminMapper;
import com.elmlite.platform.mapper.WalletMapper;
import com.elmlite.platform.mapper.WalletRechargeRuleMapper;
import com.elmlite.platform.mapper.WalletTransactionMapper;
import com.elmlite.platform.wallet.domain.VirtualWallet;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class WalletRechargeService {
    public static final long MAX_AMOUNT_CENT = 9_999_999_999L;
    private final WalletAccountService accounts;
    private final WalletMapper wallets;
    private final WalletTransactionMapper transactions;
    private final WalletRechargeRuleMapper rules;
    private final AdminMapper admins;

    public WalletRechargeService(WalletAccountService accounts, WalletMapper wallets,
                                 WalletTransactionMapper transactions, WalletRechargeRuleMapper rules,
                                 AdminMapper admins) {
        this.accounts = accounts;
        this.wallets = wallets;
        this.transactions = transactions;
        this.rules = rules;
        this.admins = admins;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public RechargeView recharge(String ownerType, long ownerId, long amountCent, String requestId) {
        if (amountCent <= 0 || amountCent > MAX_AMOUNT_CENT || requestId == null
                || !requestId.matches("[A-Za-z0-9_-]{1,64}")) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "充值参数无效");
        }
        accounts.get(ownerType, ownerId);
        WalletEntity wallet = wallets.lockByOwner(ownerType, ownerId);
        if (wallet == null) throw new BusinessException(HttpStatus.NOT_FOUND, "钱包不存在");

        WalletTransaction existing = transactions.findByRequest(wallet.getId(), requestId);
        if (existing != null) {
            if (!"RECHARGE".equals(existing.getTransactionType())
                    || !Objects.equals(wallet.getId(), existing.getDestinationWalletId())
                    || !Long.valueOf(amountCent).equals(existing.getAmountCent())) {
                throw new BusinessException(HttpStatus.CONFLICT, "请求编号已用于其他交易");
            }
            return RechargeView.from(existing);
        }

        WalletRechargeRule rule = requireRule();
        long reward = Integer.valueOf(1).equals(rule.getEnabled()) && amountCent >= rule.getThresholdCent()
                ? rule.getRewardCent() : 0L;
        VirtualWallet domain = new VirtualWallet(wallet.getBalanceCent());
        try {
            domain.credit(Math.addExact(amountCent, reward));
        } catch (ArithmeticException ex) {
            throw new BusinessException(HttpStatus.CONFLICT, "金额超过支持范围");
        }
        wallet.setBalanceCent(domain.getBalanceCent());
        if (wallets.updateById(wallet) != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, "钱包状态已变化");
        }

        WalletTransaction transaction = new WalletTransaction();
        transaction.setTransactionNo(UUID.randomUUID().toString());
        transaction.setTransactionType("RECHARGE");
        transaction.setRequestWalletId(wallet.getId());
        transaction.setRequestKey(requestId);
        transaction.setDestinationWalletId(wallet.getId());
        transaction.setAmountCent(amountCent);
        transaction.setRewardCent(reward);
        transaction.setFeeCent(0L);
        transaction.setDestinationBalanceAfterCent(wallet.getBalanceCent());
        transactions.insert(transaction);
        return RechargeView.from(transaction);
    }

    @Transactional(readOnly = true)
    public List<TransactionView> listTransactions(String ownerType, long ownerId) {
        long walletId = accounts.get(ownerType, ownerId).id();
        return transactions.findByWallet(walletId).stream()
                .map(transaction -> TransactionView.from(transaction, walletId)).toList();
    }

    @Transactional(readOnly = true)
    public RuleView getRule(long adminId) {
        requireAdmin(adminId);
        return RuleView.from(requireRule());
    }

    @Transactional
    public RuleView updateRule(long adminId, long thresholdCent, long rewardCent, boolean enabled) {
        requireAdmin(adminId);
        if (thresholdCent <= 0 || thresholdCent > MAX_AMOUNT_CENT
                || rewardCent < 0 || rewardCent > MAX_AMOUNT_CENT) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "充值规则参数无效");
        }
        WalletRechargeRule rule = requireRule();
        rule.setThresholdCent(thresholdCent);
        rule.setRewardCent(rewardCent);
        rule.setEnabled(enabled ? 1 : 0);
        if (rules.updateById(rule) != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, "充值规则已变化");
        }
        return RuleView.from(rule);
    }

    private WalletRechargeRule requireRule() {
        WalletRechargeRule rule = rules.selectById(1L);
        if (rule == null) throw new BusinessException(HttpStatus.CONFLICT, "充值规则尚未配置");
        return rule;
    }

    private void requireAdmin(long adminId) {
        Admin admin = admins.selectById(adminId);
        if (admin == null || !Integer.valueOf(1).equals(admin.getStatus())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "管理员账号不可用");
        }
    }

    public record RechargeView(long transactionId, long amountCent, long rewardCent, long balanceAfterCent) {
        static RechargeView from(WalletTransaction transaction) {
            return new RechargeView(transaction.getId(), transaction.getAmountCent(),
                    transaction.getRewardCent(), transaction.getDestinationBalanceAfterCent());
        }
    }

    public record RuleView(long thresholdCent, long rewardCent, boolean enabled) {
        static RuleView from(WalletRechargeRule rule) {
            return new RuleView(rule.getThresholdCent(), rule.getRewardCent(), Integer.valueOf(1).equals(rule.getEnabled()));
        }
    }

    public record TransactionView(long id, String transactionNo, String transactionType,
                                  Long sourceWalletId, Long destinationWalletId, long amountCent,
                                  long rewardCent, long feeCent, Long balanceAfterCent, LocalDateTime createdAt) {
        static TransactionView from(WalletTransaction transaction, long walletId) {
            Long balanceAfter = Long.valueOf(walletId).equals(transaction.getSourceWalletId())
                    ? transaction.getSourceBalanceAfterCent() : transaction.getDestinationBalanceAfterCent();
            return new TransactionView(transaction.getId(), transaction.getTransactionNo(), transaction.getTransactionType(),
                    transaction.getSourceWalletId(), transaction.getDestinationWalletId(), transaction.getAmountCent(),
                    transaction.getRewardCent(), transaction.getFeeCent(), balanceAfter, transaction.getCreatedAt());
        }
    }
}
