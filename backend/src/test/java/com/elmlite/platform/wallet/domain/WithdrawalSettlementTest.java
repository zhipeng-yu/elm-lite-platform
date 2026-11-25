package com.elmlite.platform.wallet.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WithdrawalSettlementTest {

    @Test
    void feeIncludesRewardInsteadOfAddingItTwice() {
        var result = new WithdrawalSettlement(1000).quote(110_000L, 10_000L);

        assertEquals(110_000L, result.amountCent());
        assertEquals(11_000L, result.feeCent());
        assertEquals(99_000L, result.netCent());
        assertEquals(10_000L, result.rewardForfeitedCent());
    }

    @Test
    void forfeitsRewardWhenItExceedsPercentageFee() {
        var result = new WithdrawalSettlement(1000).quote(100_000L, 20_000L);

        assertEquals(20_000L, result.feeCent());
        assertEquals(80_000L, result.netCent());
        assertEquals(20_000L, result.rewardForfeitedCent());
    }

    @Test
    void appliesPercentageFeeWhenItExceedsReward() {
        var result = new WithdrawalSettlement(1000).quote(100_000L, 5_000L);

        assertEquals(10_000L, result.feeCent());
        assertEquals(90_000L, result.netCent());
        assertEquals(5_000L, result.rewardForfeitedCent());
    }

    @Test
    void supportsNoFeeAndNoReward() {
        var result = new WithdrawalSettlement(0).quote(100_000L, 0L);

        assertEquals(0L, result.feeCent());
        assertEquals(100_000L, result.netCent());
        assertEquals(0L, result.rewardForfeitedCent());
    }

    @Test
    void rejectsNegativeReward() {
        var policy = new WithdrawalSettlement(1000);

        assertThrows(IllegalArgumentException.class,
                () -> policy.quote(100_000L, -1L));
    }

    @Test
    void rejectsZeroPayout() {
        var policy = new WithdrawalSettlement(1000);

        assertThrows(IllegalArgumentException.class,
                () -> policy.quote(1000L, 1000L));
    }

    @Test
    void rejectsNonPositiveAmount() {
        var policy = new WithdrawalSettlement(1000);

        assertThrows(IllegalArgumentException.class,
                () -> policy.quote(0L, 0L));
    }
}
