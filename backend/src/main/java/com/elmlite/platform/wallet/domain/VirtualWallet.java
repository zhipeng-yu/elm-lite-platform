package com.elmlite.platform.wallet.domain;

public final class VirtualWallet {
    private long balanceCent;
    private long frozenBalanceCent;

    public VirtualWallet(long balanceCent) {
        this.balanceCent = balanceCent;
    }

    public VirtualWallet(long balanceCent, long frozenBalanceCent) {
        if (frozenBalanceCent < 0 || frozenBalanceCent > balanceCent) {
            throw new IllegalArgumentException("冻结金额与钱包余额不一致");
        }
        this.balanceCent = balanceCent;
        this.frozenBalanceCent = frozenBalanceCent;
    }

    public long getBalanceCent() {
        return balanceCent;
    }

    public void credit(long amountCent) {
        if (amountCent <= 0) {
            throw new IllegalArgumentException("入账金额必须大于零");
        }

        balanceCent = Math.addExact(balanceCent, amountCent);
    }

    public void debit(long amountCent) {
        if (amountCent <= 0) {
            throw new IllegalArgumentException("扣款金额必须大于零");
        }

        if (amountCent > getAvailableBalanceCent()) {
            throw new IllegalStateException("钱包余额不足");
        }

        balanceCent -= amountCent;
    }

    public long getFrozenBalanceCent() {
        return frozenBalanceCent;
    }

    public long getAvailableBalanceCent() {
        return balanceCent - frozenBalanceCent;
    }

    public void freeze(long amountCent) {
        if (amountCent <= 0) {
            throw new IllegalArgumentException("冻结金额必须大于零");
        }
        if (amountCent > getAvailableBalanceCent()) {
            throw new IllegalStateException("可用余额不足");
        }
        frozenBalanceCent += amountCent;
    }

    public void unfreeze(long amountCent) {
        if (amountCent <= 0) {
            throw new IllegalArgumentException("解冻金额必须大于零");
        }
        if (amountCent > frozenBalanceCent) {
            throw new IllegalStateException("冻结金额不足");
        }
        frozenBalanceCent -= amountCent;
    }
}