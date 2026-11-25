package com.elmlite.platform.service;

import com.elmlite.platform.entity.WalletCredit;
import com.elmlite.platform.entity.WalletEntity;
import com.elmlite.platform.entity.WalletTransaction;
import com.elmlite.platform.exception.BusinessException;
import com.elmlite.platform.mapper.WalletCreditMapper;
import com.elmlite.platform.mapper.WalletMapper;
import com.elmlite.platform.mapper.WalletTransactionMapper;
import com.elmlite.platform.wallet.domain.OverdraftPolicy;
import com.elmlite.platform.wallet.domain.VirtualWallet;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@Service
public class WalletCreditOperationService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final WalletAccountService accounts;
    private final WalletMapper wallets;
    private final WalletCreditMapper credits;
    private final WalletTransactionMapper transactions;
    private final WalletWithdrawalService withdrawals;

    public WalletCreditOperationService(
            WalletAccountService accounts,
            WalletMapper wallets,
            WalletCreditMapper credits,
            WalletTransactionMapper transactions,
            WalletWithdrawalService withdrawals) {
        this.accounts = accounts;
        this.wallets = wallets;
        this.credits = credits;
        this.transactions = transactions;
        this.withdrawals = withdrawals;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public OperationView borrow(
            String ownerType, long ownerId, long amountCent, String requestId) {
        validate(ownerType, amountCent, requestId);

        WalletEntity wallet = lockWallet(ownerType, ownerId);
        WalletTransaction existing = transactions.findByRequest(wallet.getId(), requestId);

        if (existing != null) {
            return replay(existing, "BORROW", amountCent, wallet.getId());
        }

        WalletCredit credit = requireCredit(wallet.getId());
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        accrueInterest(credit, today);

        long totalDebt = totalDebt(credit);

        if (credit.getPrincipalCent() == 0 && credit.getAccruedInterestCent() > 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "Repay interest before borrowing");
        }

        OverdraftPolicy policy = policy(credit);

        try {
            policy.borrow(totalDebt, amountCent);
        } catch (IllegalStateException exception) {
            throw new BusinessException(HttpStatus.CONFLICT, "Credit limit exceeded");
        }

        VirtualWallet domain = new VirtualWallet(
                wallet.getBalanceCent(), wallet.getFrozenBalanceCent());

        try {
            domain.credit(amountCent);
        } catch (ArithmeticException exception) {
            throw new BusinessException(HttpStatus.CONFLICT, "Wallet amount overflow");
        }

        if (totalDebt == 0) {
            LocalDate due = policy.dueDate(today);
            credit.setDueDate(due);
            credit.setInterestCalculatedThrough(due);
        }

        credit.setPrincipalCent(Math.addExact(credit.getPrincipalCent(), amountCent));
        wallet.setBalanceCent(domain.getBalanceCent());
        save(wallet, credit);

        WalletTransaction transaction = baseTransaction(
                wallet.getId(), "BORROW", amountCent, requestId);
        transaction.setDestinationWalletId(wallet.getId());
        transaction.setDestinationBalanceAfterCent(wallet.getBalanceCent());

        transactions.insert(transaction);
        return view(transaction);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public OperationView repay(
            String ownerType, long ownerId, long amountCent, String requestId) {
        validate(ownerType, amountCent, requestId);

        WalletEntity wallet = lockWallet(ownerType, ownerId);
        WalletTransaction existing = transactions.findByRequest(wallet.getId(), requestId);

        if (existing != null) {
            return replay(existing, "REPAY", amountCent, wallet.getId());
        }

        WalletCredit credit = requireCredit(wallet.getId());
        accrueInterest(credit, LocalDate.now(BUSINESS_ZONE));

        long debt = totalDebt(credit);

        if (debt == 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "No outstanding debt");
        }

        if (amountCent > debt) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Repayment exceeds debt");
        }

        long available = wallet.getBalanceCent() - wallet.getFrozenBalanceCent();
        long reward = withdrawals.outstandingReward(wallet.getId());
        long repayable = reward >= available ? 0L : available - reward;

        if (amountCent > repayable) {
            throw new BusinessException(HttpStatus.CONFLICT, "Insufficient non-reward balance");
        }

        VirtualWallet domain = new VirtualWallet(
                wallet.getBalanceCent(), wallet.getFrozenBalanceCent());
        domain.debit(amountCent);

        long interestPaid = Math.min(amountCent, credit.getAccruedInterestCent());
        long principalPaid = amountCent - interestPaid;

        credit.setAccruedInterestCent(credit.getAccruedInterestCent() - interestPaid);
        credit.setPrincipalCent(credit.getPrincipalCent() - principalPaid);

        if (credit.getPrincipalCent() == 0 && credit.getAccruedInterestCent() == 0) {
            credit.setDueDate(null);
            credit.setInterestCalculatedThrough(null);
        }

        wallet.setBalanceCent(domain.getBalanceCent());
        save(wallet, credit);

        WalletTransaction transaction = baseTransaction(
                wallet.getId(), "REPAY", amountCent, requestId);
        transaction.setSourceWalletId(wallet.getId());
        transaction.setSourceBalanceAfterCent(wallet.getBalanceCent());
        transaction.setFeeCent(interestPaid);

        transactions.insert(transaction);
        return view(transaction);
    }

    private void validate(String ownerType, long amountCent, String requestId) {
        if (!"USER".equals(ownerType)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Credit is available to users only");
        }

        if (amountCent <= 0 || amountCent > WalletRechargeService.MAX_AMOUNT_CENT
                || requestId == null || !requestId.matches("[A-Za-z0-9_-]{1,64}")) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Invalid credit operation");
        }
    }

    private WalletEntity lockWallet(String ownerType, long ownerId) {
        long expectedId = accounts.get(ownerType, ownerId).id();
        WalletEntity wallet = wallets.lockByOwner(ownerType, ownerId);

        if (wallet == null || wallet.getId().longValue() != expectedId) {
            throw new BusinessException(HttpStatus.CONFLICT, "Wallet changed");
        }

        return wallet;
    }

    private WalletCredit requireCredit(long walletId) {
        WalletCredit credit = credits.selectById(walletId);

        if (credit == null) {
            throw new BusinessException(HttpStatus.CONFLICT, "Credit account is not configured");
        }

        return credit;
    }

    private OverdraftPolicy policy(WalletCredit credit) {
        return new OverdraftPolicy(
                credit.getCreditLimitCent(),
                credit.getRepaymentDays(),
                credit.getDailyRateBasisPoints());
    }

    private void accrueInterest(WalletCredit credit, LocalDate today) {
        if (credit.getPrincipalCent() == 0) {
            return;
        }

        LocalDate due = credit.getDueDate();

        if (due == null) {
            throw new BusinessException(HttpStatus.CONFLICT, "Debt has no due date");
        }

        LocalDate start = due;
        LocalDate through = credit.getInterestCalculatedThrough();

        if (through != null && through.isAfter(start)) {
            start = through;
        }

        if (!today.isAfter(start)) {
            return;
        }

        try {
            long interest = policy(credit).interestCent(
                    credit.getPrincipalCent(), start, today);

            credit.setAccruedInterestCent(
                    Math.addExact(credit.getAccruedInterestCent(), interest));
            credit.setInterestCalculatedThrough(today);

        } catch (ArithmeticException exception) {
            throw new BusinessException(HttpStatus.CONFLICT, "Interest amount overflow");
        }
    }

    private long totalDebt(WalletCredit credit) {
        try {
            return Math.addExact(
                    credit.getPrincipalCent(), credit.getAccruedInterestCent());
        } catch (ArithmeticException exception) {
            throw new BusinessException(HttpStatus.CONFLICT, "Debt amount overflow");
        }
    }

    private void save(WalletEntity wallet, WalletCredit credit) {
        if (wallets.updateById(wallet) != 1 || credits.updateDebt(credit) != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, "Wallet or credit changed");
        }
    }

    private WalletTransaction baseTransaction(
            long walletId, String type, long amountCent, String requestId) {
        WalletTransaction transaction = new WalletTransaction();
        transaction.setTransactionNo(UUID.randomUUID().toString());
        transaction.setTransactionType(type);
        transaction.setRequestWalletId(walletId);
        transaction.setRequestKey(requestId);
        transaction.setAmountCent(amountCent);
        transaction.setRewardCent(0L);
        transaction.setFeeCent(0L);
        return transaction;
    }

    private OperationView replay(
            WalletTransaction transaction, String type, long amountCent, long walletId) {

        boolean walletMatches = "BORROW".equals(type)
                ? Long.valueOf(walletId).equals(transaction.getDestinationWalletId())
                : Long.valueOf(walletId).equals(transaction.getSourceWalletId());

        if (!type.equals(transaction.getTransactionType())
                || !Long.valueOf(amountCent).equals(transaction.getAmountCent())
                || !walletMatches) {
            throw new BusinessException(HttpStatus.CONFLICT, "Request ID already used");
        }

        return view(transaction);
    }

    private OperationView view(WalletTransaction transaction) {
        boolean repayment = "REPAY".equals(transaction.getTransactionType());
        long interestPaid = repayment ? transaction.getFeeCent() : 0L;
        long principalPaid = repayment ? transaction.getAmountCent() - interestPaid : 0L;
        long balanceAfter = repayment
                ? transaction.getSourceBalanceAfterCent()
                : transaction.getDestinationBalanceAfterCent();

        return new OperationView(
                transaction.getId(),
                transaction.getAmountCent(),
                principalPaid,
                interestPaid,
                balanceAfter);
    }

    public record OperationView(
            long transactionId,
            long amountCent,
            long principalPaidCent,
            long interestPaidCent,
            long balanceAfterCent) {
    }
}
