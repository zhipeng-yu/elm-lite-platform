package com.elmlite.platform.wallet.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VirtualWalletFreezeTest {

    @Test
    void freezeReducesAvailableBalanceWithoutChangingTotal() {
        VirtualWallet wallet = new VirtualWallet(1_000L);

        wallet.freeze(300L);

        assertEquals(1_000L, wallet.getBalanceCent());
        assertEquals(300L, wallet.getFrozenBalanceCent());
        assertEquals(700L, wallet.getAvailableBalanceCent());
    }

    @Test
    void debitCannotUseFrozenMoney() {
        VirtualWallet wallet = new VirtualWallet(1_000L);
        wallet.freeze(300L);

        assertThrows(IllegalStateException.class, () -> wallet.debit(701L));
        assertEquals(1_000L, wallet.getBalanceCent());

        wallet.debit(700L);

        assertEquals(300L, wallet.getBalanceCent());
        assertEquals(300L, wallet.getFrozenBalanceCent());
        assertEquals(0L, wallet.getAvailableBalanceCent());
    }

    @Test
    void unfreezeRestoresAvailableBalance() {
        VirtualWallet wallet = new VirtualWallet(1_000L);
        wallet.freeze(300L);

        wallet.unfreeze(100L);

        assertEquals(1_000L, wallet.getBalanceCent());
        assertEquals(200L, wallet.getFrozenBalanceCent());
        assertEquals(800L, wallet.getAvailableBalanceCent());
    }

    @Test
    void creditKeepsFrozenMoneyUnchanged() {
        VirtualWallet wallet = new VirtualWallet(1_000L);
        wallet.freeze(300L);

        wallet.credit(200L);

        assertEquals(1_200L, wallet.getBalanceCent());
        assertEquals(300L, wallet.getFrozenBalanceCent());
        assertEquals(900L, wallet.getAvailableBalanceCent());
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L, Long.MIN_VALUE})
    void freezeRejectsNonPositiveAmount(long amountCent) {
        VirtualWallet wallet = new VirtualWallet(1_000L);

        assertThrows(IllegalArgumentException.class, () -> wallet.freeze(amountCent));

        assertEquals(1_000L, wallet.getBalanceCent());
        assertEquals(0L, wallet.getFrozenBalanceCent());
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L, Long.MIN_VALUE})
    void unfreezeRejectsNonPositiveAmount(long amountCent) {
        VirtualWallet wallet = new VirtualWallet(1_000L);
        wallet.freeze(300L);

        assertThrows(IllegalArgumentException.class, () -> wallet.unfreeze(amountCent));

        assertEquals(300L, wallet.getFrozenBalanceCent());
    }

    @Test
    void freezeRejectsAmountGreaterThanAvailableBalance() {
        VirtualWallet wallet = new VirtualWallet(1_000L);
        wallet.freeze(300L);

        assertThrows(IllegalStateException.class, () -> wallet.freeze(701L));

        assertEquals(300L, wallet.getFrozenBalanceCent());
        assertEquals(700L, wallet.getAvailableBalanceCent());
    }

    @Test
    void unfreezeRejectsAmountGreaterThanFrozenBalance() {
        VirtualWallet wallet = new VirtualWallet(1_000L);
        wallet.freeze(300L);

        assertThrows(IllegalStateException.class, () -> wallet.unfreeze(301L));

        assertEquals(300L, wallet.getFrozenBalanceCent());
        assertEquals(700L, wallet.getAvailableBalanceCent());
    }
}
