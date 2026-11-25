package com.elmlite.platform.wallet.domain;

public final class WithdrawalSettlement {

    private final WithdrawalPolicy feePolicy;

    public WithdrawalSettlement(int feeBasisPoints) {
        this.feePolicy = new WithdrawalPolicy(feeBasisPoints);
    }

    public Quote quote(long amountCent, long availableRewardCent) {
        if (availableRewardCent < 0) {
            throw new IllegalArgumentException("Reward cannot be negative");
        }

        WithdrawalPolicy.Quote basic = feePolicy.quote(amountCent);

        long rewardForfeitedCent = Math.min(amountCent, availableRewardCent);
        long feeCent = Math.max(basic.feeCent(), rewardForfeitedCent);

        if (feeCent >= amountCent) {
            throw new IllegalArgumentException("Withdrawal payout must be positive");
        }

        return new Quote(
                amountCent,
                feeCent,
                amountCent - feeCent,
                rewardForfeitedCent
        );
    }

    public record Quote(
            long amountCent,
            long feeCent,
            long netCent,
            long rewardForfeitedCent
    ) {
    }
}
