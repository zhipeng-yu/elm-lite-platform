package com.elmlite.platform.service;

import com.elmlite.platform.entity.WalletCredit;
import com.elmlite.platform.entity.WalletEntity;
import com.elmlite.platform.entity.WalletTransaction;
import com.elmlite.platform.exception.BusinessException;
import com.elmlite.platform.mapper.WalletCreditMapper;
import com.elmlite.platform.mapper.WalletMapper;
import com.elmlite.platform.mapper.WalletTransactionMapper;
import com.elmlite.platform.wallet.domain.VirtualWallet;
import com.elmlite.platform.wallet.domain.WithdrawalSettlement;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class WalletWithdrawalService {

    private final WalletAccountService accounts;
    private final WalletMapper wallets;
    private final WalletTransactionMapper transactions;
    private final WalletWithdrawalRuleService withdrawalRules;
    private final WalletCreditMapper credits;

    public WalletWithdrawalService(
            WalletAccountService accounts,
            WalletMapper wallets,
            WalletTransactionMapper transactions,
            WalletWithdrawalRuleService withdrawalRules,
            WalletCreditMapper credits) {
        this.accounts = accounts;
        this.wallets = wallets;
        this.transactions = transactions;
        this.withdrawalRules = withdrawalRules;
        this.credits = credits;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public WithdrawalView withdraw(
            String ownerType,
            long ownerId,
            long amountCent,
            String requestId) {

        if (amountCent <= 0 || amountCent > WalletRechargeService.MAX_AMOUNT_CENT
                || requestId == null || !requestId.matches("[A-Za-z0-9_-]{1,64}")) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Invalid withdrawal request");
        }

        long accountWalletId = accounts.get(ownerType, ownerId).id();
        WalletEntity wallet = wallets.lockByOwner(ownerType, ownerId);

        if (wallet == null || wallet.getId().longValue() != accountWalletId) {
            throw new BusinessException(HttpStatus.CONFLICT, "Wallet changed");
        }

        WalletTransaction existing = transactions.findByRequest(wallet.getId(), requestId);

        if (existing != null) {
            if (!"WITHDRAWAL".equals(existing.getTransactionType())
                    || !wallet.getId().equals(existing.getSourceWalletId())
                    || !Long.valueOf(amountCent).equals(existing.getAmountCent())) {
                throw new BusinessException(HttpStatus.CONFLICT, "Request ID already used");
            }

            return view(existing);
        }

        WalletCredit credit = credits.selectById(wallet.getId());

        if (credit != null
                && (credit.getPrincipalCent() > 0 || credit.getAccruedInterestCent() > 0)) {
            throw new BusinessException(HttpStatus.CONFLICT, "Repay debt before withdrawal");
        }

        long rewardCent = outstandingReward(wallet.getId());
        WithdrawalSettlement.Quote quote;

        try {
            quote = new WithdrawalSettlement(withdrawalRules.currentFeeBasisPoints()).quote(amountCent, rewardCent);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Withdrawal amount is insufficient after deductions");
        }

        VirtualWallet domain = new VirtualWallet(
                wallet.getBalanceCent(),
                wallet.getFrozenBalanceCent());

        try {
            domain.debit(amountCent);
        } catch (IllegalStateException exception) {
            throw new BusinessException(HttpStatus.CONFLICT, "Insufficient available balance");
        }

        wallet.setBalanceCent(domain.getBalanceCent());

        if (wallets.updateById(wallet) != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, "Wallet changed");
        }

        WalletTransaction transaction = new WalletTransaction();
        transaction.setTransactionNo(UUID.randomUUID().toString());
        transaction.setTransactionType("WITHDRAWAL");
        transaction.setRequestWalletId(wallet.getId());
        transaction.setRequestKey(requestId);
        transaction.setSourceWalletId(wallet.getId());
        transaction.setAmountCent(amountCent);
        transaction.setRewardCent(quote.rewardForfeitedCent());
        transaction.setFeeCent(quote.feeCent());
        transaction.setSourceBalanceAfterCent(wallet.getBalanceCent());

        transactions.insert(transaction);
        return view(transaction);
    }

    long outstandingReward(long walletId) {
        List<WalletTransaction> history = transactions.findByWallet(walletId);
        long reward = 0L;

        for (int index = history.size() - 1; index >= 0; index--) {
            WalletTransaction transaction = history.get(index);
            boolean source = Long.valueOf(walletId).equals(transaction.getSourceWalletId());
            boolean destination = Long.valueOf(walletId).equals(transaction.getDestinationWalletId());

            if ("RECHARGE".equals(transaction.getTransactionType()) && destination) {
                try {
                    reward = Math.addExact(reward, transaction.getRewardCent());
                } catch (ArithmeticException exception) {
                    throw new BusinessException(HttpStatus.CONFLICT, "Reward history overflow");
                }
            } else if ("PAYMENT".equals(transaction.getTransactionType()) && source) {
                long spent = transaction.getAmountCent();
                reward = spent >= reward ? 0L : reward - spent;
            } else if ("WITHDRAWAL".equals(transaction.getTransactionType()) && source) {
                long forfeited = transaction.getRewardCent();
                reward = forfeited >= reward ? 0L : reward - forfeited;
            }
        }

        return reward;
    }

    private WithdrawalView view(WalletTransaction transaction) {
        return new WithdrawalView(
                transaction.getId(),
                transaction.getAmountCent(),
                transaction.getFeeCent(),
                transaction.getAmountCent() - transaction.getFeeCent(),
                transaction.getRewardCent(),
                transaction.getSourceBalanceAfterCent());
    }

    public record WithdrawalView(
            long transactionId,
            long amountCent,
            long feeCent,
            long netCent,
            long rewardForfeitedCent,
            long balanceAfterCent) {
    }
}
