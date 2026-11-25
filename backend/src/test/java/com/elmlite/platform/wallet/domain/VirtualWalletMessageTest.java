package com.elmlite.platform.wallet.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VirtualWalletMessageTest {
    @Test
    void invalidUnfreezeAmountUsesCorrectMessage() {
        VirtualWallet wallet = new VirtualWallet(1_000L);
        var exception = assertThrows(IllegalArgumentException.class, () -> wallet.unfreeze(0L));
        assertEquals("解冻金额必须大于零", exception.getMessage());
    }
}
