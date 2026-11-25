package com.elmlite.platform.wallet.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WithdrawalPolicyTest {

    @Test
    void calculatesTenPercentFee() {
        var quote = new WithdrawalPolicy(1000).quote(100_000L);

        assertEquals(100_000L, quote.amountCent());
        assertEquals(10_000L, quote.feeCent());
        assertEquals(90_000L, quote.netCent());
    }

    @Test
    void roundsFeeUpToTheNextCent() {
        var quote = new WithdrawalPolicy(1000).quote(101L);

        assertEquals(11L, quote.feeCent());
        assertEquals(90L, quote.netCent());
    }

    @Test
    void supportsZeroFee() {
        var quote = new WithdrawalPolicy(0).quote(100L);

        assertEquals(0L, quote.feeCent());
        assertEquals(100L, quote.netCent());
    }

    @Test
    void rejectsNonPositiveAmounts() {
        var policy = new WithdrawalPolicy(1000);

        assertThrows(IllegalArgumentException.class, () -> policy.quote(0L));
        assertThrows(IllegalArgumentException.class, () -> policy.quote(-1L));
    }

    @Test
    void rejectsInvalidRates() {
        assertThrows(IllegalArgumentException.class, () -> new WithdrawalPolicy(-1));
        assertThrows(IllegalArgumentException.class, () -> new WithdrawalPolicy(10000));
    }

    @Test
    void rejectsAWithdrawalWithNoPayout() {
        var policy = new WithdrawalPolicy(1000);

        assertThrows(IllegalArgumentException.class, () -> policy.quote(1L));
    }

    @Test
    void handlesLargeAmountsWithoutOverflow() {
        long amount = Long.MAX_VALUE;
        long expectedFee = amount / 10L + 1L;

        var quote = new WithdrawalPolicy(1000).quote(amount);

        assertEquals(expectedFee, quote.feeCent());
        assertEquals(amount - expectedFee, quote.netCent());
    }
}