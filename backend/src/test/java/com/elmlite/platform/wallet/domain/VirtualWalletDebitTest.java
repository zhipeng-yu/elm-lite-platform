package com.elmlite.platform.wallet.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VirtualWalletDebitTest {

    @Test
    void debitSubtractsAmountFromBalance() {
        VirtualWallet wallet = new VirtualWallet(1_000L);

        wallet.debit(250L);

        assertEquals(750L, wallet.getBalanceCent());
    }

    @Test
    void debitCanUseEntireBalance() {
        VirtualWallet wallet = new VirtualWallet(1_000L);

        wallet.debit(1_000L);

        assertEquals(0L, wallet.getBalanceCent());
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L, Long.MIN_VALUE})
    void debitRejectsNonPositiveAmountWithoutChangingBalance(long amountCent) {
        VirtualWallet wallet = new VirtualWallet(1_000L);

        assertThrows(IllegalArgumentException.class, () -> wallet.debit(amountCent));

        assertEquals(1_000L, wallet.getBalanceCent());
    }

    @Test
    void debitRejectsInsufficientBalanceWithoutChangingBalance() {
        VirtualWallet wallet = new VirtualWallet(1_000L);

        assertThrows(IllegalStateException.class, () -> wallet.debit(1_001L));

        assertEquals(1_000L, wallet.getBalanceCent());
    }
}
