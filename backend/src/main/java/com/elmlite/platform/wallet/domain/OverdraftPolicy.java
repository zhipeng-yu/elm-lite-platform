package com.elmlite.platform.wallet.domain;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public final class OverdraftPolicy {

    private static final BigInteger RATE_BASE = BigInteger.valueOf(10_000L);

    private final long creditLimitCent;
    private final int repaymentDays;
    private final int dailyRateBasisPoints;

    public OverdraftPolicy(
            long creditLimitCent,
            int repaymentDays,
            int dailyRateBasisPoints) {

        if (creditLimitCent < 0 || repaymentDays < 0
                || dailyRateBasisPoints < 0 || dailyRateBasisPoints > 10000) {
            throw new IllegalArgumentException("Invalid overdraft policy");
        }

        this.creditLimitCent = creditLimitCent;
        this.repaymentDays = repaymentDays;
        this.dailyRateBasisPoints = dailyRateBasisPoints;
    }

    public long availableCreditCent(long principalCent) {
        if (principalCent < 0) {
            throw new IllegalArgumentException("Principal cannot be negative");
        }

        return principalCent >= creditLimitCent
                ? 0L : creditLimitCent - principalCent;
    }

    public long borrow(long principalCent, long amountCent) {
        if (amountCent <= 0) {
            throw new IllegalArgumentException("Borrow amount must be positive");
        }

        if (amountCent > availableCreditCent(principalCent)) {
            throw new IllegalStateException("Credit limit exceeded");
        }

        return Math.addExact(principalCent, amountCent);
    }

    public long repay(long principalCent, long amountCent) {
        if (principalCent < 0 || amountCent <= 0 || amountCent > principalCent) {
            throw new IllegalArgumentException("Invalid principal repayment");
        }

        return principalCent - amountCent;
    }

    public LocalDate dueDate(LocalDate borrowedOn) {
        return Objects.requireNonNull(borrowedOn, "borrowedOn")
                .plusDays(repaymentDays);
    }

    public long interestCent(
            long principalCent,
            LocalDate dueDate,
            LocalDate asOfDate) {

        if (principalCent < 0) {
            throw new IllegalArgumentException("Principal cannot be negative");
        }

        Objects.requireNonNull(dueDate, "dueDate");
        Objects.requireNonNull(asOfDate, "asOfDate");

        if (principalCent == 0 || dailyRateBasisPoints == 0
                || !asOfDate.isAfter(dueDate)) {
            return 0L;
        }

        long overdueDays = ChronoUnit.DAYS.between(dueDate, asOfDate);

        BigInteger numerator = BigInteger.valueOf(principalCent)
                .multiply(BigInteger.valueOf(dailyRateBasisPoints))
                .multiply(BigInteger.valueOf(overdueDays));

        return numerator
                .add(RATE_BASE.subtract(BigInteger.ONE))
                .divide(RATE_BASE)
                .longValueExact();
    }
}
