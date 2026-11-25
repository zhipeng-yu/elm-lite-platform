package com.elmlite.platform.wallet.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VirtualWalletRestoreTest {
    @Test
    void restoresFrozenBalanceAndKeepsItUnavailable() {
        VirtualWallet wallet = new VirtualWallet(1_000L, 600L);
        assertEquals(600L, wallet.getFrozenBalanceCent());
        assertEquals(400L, wallet.getAvailableBalanceCent());
        assertThrows(IllegalStateException.class, () -> wallet.debit(401L));
        wallet.debit(400L);
        assertEquals(600L, wallet.getBalanceCent());
        assertEquals(0L, wallet.getAvailableBalanceCent());
    }

    @Test
    void rejectsImpossibleFrozenBalance() {
        assertThrows(IllegalArgumentException.class, () -> new VirtualWallet(10L, -1L));
        assertThrows(IllegalArgumentException.class, () -> new VirtualWallet(10L, 11L));
    }
}
