package com.elmlite.platform.wallet.domain;

public final class WithdrawalPolicy {

    private static final long BASIS_POINTS = 10_000L;
    private final int feeBasisPoints;

    public WithdrawalPolicy(int feeBasisPoints) {
        if (feeBasisPoints < 0 || feeBasisPoints >= BASIS_POINTS) {
            throw new IllegalArgumentException("Invalid withdrawal fee rate");
        }

        this.feeBasisPoints = feeBasisPoints;
    }

    public Quote quote(long amountCent) {
        if (amountCent <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }

        long feeCent =
                (amountCent / BASIS_POINTS) * feeBasisPoints
                + ((amountCent % BASIS_POINTS) * feeBasisPoints + BASIS_POINTS - 1)
                / BASIS_POINTS;

        if (feeCent >= amountCent) {
            throw new IllegalArgumentException("Withdrawal payout must be positive");
        }

        return new Quote(amountCent, feeCent, amountCent - feeCent);
    }

    public record Quote(long amountCent, long feeCent, long netCent) {
    }
}