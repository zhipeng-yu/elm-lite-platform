package com.elmlite.platform.wallet.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VirtualWalletTest {

    @Test
    void creditAddsPositiveAmountInCents() {
        VirtualWallet wallet = new VirtualWallet(1_000L);

        wallet.credit(250L);

        assertEquals(1_250L, wallet.getBalanceCent());
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L, Long.MIN_VALUE})
    void creditRejectsNonPositiveAmountWithoutChangingBalance(long amountCent) {
        VirtualWallet wallet = new VirtualWallet(1_000L);

        assertThrows(IllegalArgumentException.class, () -> wallet.credit(amountCent));

        assertEquals(1_000L, wallet.getBalanceCent());
    }

    @Test
    void creditRejectsOverflowWithoutChangingBalance() {
        VirtualWallet wallet = new VirtualWallet(Long.MAX_VALUE - 1L);

        assertThrows(ArithmeticException.class, () -> wallet.credit(2L));

        assertEquals(Long.MAX_VALUE - 1L, wallet.getBalanceCent());
    }
}
