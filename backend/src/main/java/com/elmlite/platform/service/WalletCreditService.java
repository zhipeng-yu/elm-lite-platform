package com.elmlite.platform.service;

import com.elmlite.platform.entity.Admin;
import com.elmlite.platform.entity.WalletCredit;
import com.elmlite.platform.entity.WalletEntity;
import com.elmlite.platform.exception.BusinessException;
import com.elmlite.platform.mapper.AdminMapper;
import com.elmlite.platform.mapper.WalletCreditMapper;
import com.elmlite.platform.mapper.WalletMapper;
import com.elmlite.platform.wallet.domain.OverdraftPolicy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class WalletCreditService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final WalletAccountService accounts;
    private final WalletMapper wallets;
    private final WalletCreditMapper credits;
    private final AdminMapper admins;

    public WalletCreditService(
            WalletAccountService accounts,
            WalletMapper wallets,
            WalletCreditMapper credits,
            AdminMapper admins) {
        this.accounts = accounts;
        this.wallets = wallets;
        this.credits = credits;
        this.admins = admins;
    }

    @Transactional(readOnly = true)
    public CreditView get(String ownerType, long ownerId) {
        if (!"USER".equals(ownerType)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Credit is available to users only");
        }

        long walletId = accounts.get(ownerType, ownerId).id();
        WalletCredit credit = credits.selectById(walletId);

        if (credit == null) {
            return new CreditView(false, walletId, 0L, 0, 0,
                    0L, 0L, 0L, 0L, null);
        }

        return view(credit);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CreditView configure(
            long adminId,
            long walletId,
            long creditLimitCent,
            int repaymentDays,
            int dailyRateBasisPoints) {

        requireAdmin(adminId);

        if (creditLimitCent < 0 || creditLimitCent > WalletRechargeService.MAX_AMOUNT_CENT
                || repaymentDays < 1 || repaymentDays > 3650
                || dailyRateBasisPoints < 0 || dailyRateBasisPoints > 10000) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Invalid credit configuration");
        }

        WalletEntity wallet = wallets.lockById(walletId);

        if (wallet == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Wallet not found");
        }

        if (!"USER".equals(wallet.getOwnerType())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Credit is available to users only");
        }

        accounts.get(wallet.getOwnerType(), wallet.getOwnerId());

        WalletCredit credit = credits.selectById(walletId);

        if (credit == null) {
            credit = new WalletCredit();
            credit.setWalletId(walletId);
            credit.setCreditLimitCent(creditLimitCent);
            credit.setRepaymentDays(repaymentDays);
            credit.setDailyRateBasisPoints(dailyRateBasisPoints);
            credit.setPrincipalCent(0L);
            credit.setAccruedInterestCent(0L);

            credits.insert(credit);
            return view(credit);
        }

        CreditView current = view(credit);

        if (creditLimitCent < current.totalDebtCent()) {
            throw new BusinessException(HttpStatus.CONFLICT, "Limit is below outstanding debt");
        }

        if (current.totalDebtCent() > 0
                && (credit.getRepaymentDays() != repaymentDays
                || credit.getDailyRateBasisPoints() != dailyRateBasisPoints)) {
            throw new BusinessException(HttpStatus.CONFLICT, "Repayment terms cannot change with debt");
        }

        credit.setCreditLimitCent(creditLimitCent);
        credit.setRepaymentDays(repaymentDays);
        credit.setDailyRateBasisPoints(dailyRateBasisPoints);

        if (credits.updateById(credit) != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, "Credit configuration changed");
        }

        return view(credit);
    }

    private CreditView view(WalletCredit credit) {
        long principal = credit.getPrincipalCent();
        long interest = credit.getAccruedInterestCent();

        OverdraftPolicy policy = new OverdraftPolicy(
                credit.getCreditLimitCent(),
                credit.getRepaymentDays(),
                credit.getDailyRateBasisPoints());

        try {
            if (principal > 0) {
                LocalDate due = credit.getDueDate();

                if (due == null) {
                    throw new BusinessException(HttpStatus.CONFLICT, "Debt has no due date");
                }

                LocalDate start = due;
                LocalDate through = credit.getInterestCalculatedThrough();

                if (through != null && through.isAfter(start)) {
                    start = through;
                }

                long newInterest = policy.interestCent(
                        principal, start, LocalDate.now(BUSINESS_ZONE));
                interest = Math.addExact(interest, newInterest);
            }

            long totalDebt = Math.addExact(principal, interest);
            long available = policy.availableCreditCent(totalDebt);

            return new CreditView(
                    true,
                    credit.getWalletId(),
                    credit.getCreditLimitCent(),
                    credit.getRepaymentDays(),
                    credit.getDailyRateBasisPoints(),
                    principal,
                    interest,
                    totalDebt,
                    available,
                    credit.getDueDate());

        } catch (ArithmeticException exception) {
            throw new BusinessException(HttpStatus.CONFLICT, "Credit amount overflow");
        }
    }

    private void requireAdmin(long adminId) {
        Admin admin = admins.selectById(adminId);

        if (admin == null || !Integer.valueOf(1).equals(admin.getStatus())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Administrator is unavailable");
        }
    }

    public record CreditView(
            boolean configured,
            long walletId,
            long creditLimitCent,
            int repaymentDays,
            int dailyRateBasisPoints,
            long principalCent,
            long interestCent,
            long totalDebtCent,
            long availableCreditCent,
            LocalDate dueDate) {
    }
}
