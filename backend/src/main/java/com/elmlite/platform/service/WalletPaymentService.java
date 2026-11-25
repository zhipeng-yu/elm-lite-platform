package com.elmlite.platform.service;

import com.elmlite.platform.entity.WalletEntity;
import com.elmlite.platform.entity.WalletPayment;
import com.elmlite.platform.entity.WalletTransaction;
import com.elmlite.platform.exception.BusinessException;
import com.elmlite.platform.mapper.WalletMapper;
import com.elmlite.platform.mapper.WalletPaymentMapper;
import com.elmlite.platform.mapper.WalletTransactionMapper;
import com.elmlite.platform.wallet.domain.VirtualWallet;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class WalletPaymentService {
    private final WalletAccountService accounts;
    private final WalletMapper wallets;
    private final WalletTransactionMapper transactions;
    private final WalletPaymentMapper payments;
    private final WalletCreditOperationService creditOperations;

    public WalletPaymentService(WalletAccountService accounts, WalletMapper wallets,
                                WalletTransactionMapper transactions, WalletPaymentMapper payments,
                                WalletCreditOperationService creditOperations) {
        this.accounts = accounts;
        this.wallets = wallets;
        this.transactions = transactions;
        this.payments = payments;
        this.creditOperations = creditOperations;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PaymentView pay(String ownerType, long ownerId, String recipientType, long recipientId,
                           long amountCent, String requestId, boolean useCredit) {
        if ((!"USER".equals(recipientType) && !"MERCHANT".equals(recipientType)) || recipientId <= 0
                || amountCent <= 0 || amountCent > WalletRechargeService.MAX_AMOUNT_CENT || requestId == null
                || !requestId.matches("[A-Za-z0-9_-]{1,64}")) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "付款参数无效");
        }
        long sourceId = accounts.get(ownerType, ownerId).id();
        WalletEntity targetRef = wallets.findByOwner(recipientType, recipientId);
        if (targetRef == null) throw new BusinessException(HttpStatus.NOT_FOUND, "收款钱包不存在");
        long targetId = targetRef.getId();
        if (sourceId == targetId) throw new BusinessException(HttpStatus.BAD_REQUEST, "不能向自己付款");

        WalletEntity low = wallets.lockById(Math.min(sourceId, targetId));
        WalletEntity high = wallets.lockById(Math.max(sourceId, targetId));
        if (low == null || high == null) throw new BusinessException(HttpStatus.NOT_FOUND, "钱包不存在");
        WalletEntity source = sourceId == low.getId() ? low : high;
        WalletEntity target = targetId == low.getId() ? low : high;
        WalletTransaction existing = transactions.findByRequest(sourceId, requestId);
        if (existing != null) {
            if (!"PAYMENT".equals(existing.getTransactionType())
                    || !Long.valueOf(targetId).equals(existing.getDestinationWalletId())
                    || !Long.valueOf(amountCent).equals(existing.getAmountCent())) {
                throw new BusinessException(HttpStatus.CONFLICT, "请求编号已用于其他交易");
            }
            return view(existing, requirePaymentState(existing.getId()));
        }
        accounts.get(recipientType, recipientId);
        long availableCent = source.getBalanceCent() - source.getFrozenBalanceCent();

        if (useCredit && "USER".equals(ownerType) && amountCent > availableCent) {
            long shortfallCent = amountCent - availableCent;

            WalletCreditOperationService.OperationView automaticBorrow =
                    creditOperations.borrow(
                            ownerType,
                            ownerId,
                            shortfallCent,
                            "auto_" + UUID.randomUUID());

            source.setBalanceCent(automaticBorrow.balanceAfterCent());
        }

        VirtualWallet from = new VirtualWallet(source.getBalanceCent(), source.getFrozenBalanceCent());
        VirtualWallet to = new VirtualWallet(target.getBalanceCent(), target.getFrozenBalanceCent());
        try {
            from.debit(amountCent);
            to.credit(amountCent);
            to.freeze(amountCent);
        } catch (IllegalStateException exception) {
            throw new BusinessException(HttpStatus.CONFLICT, "可用余额不足");
        } catch (ArithmeticException exception) {
            throw new BusinessException(HttpStatus.CONFLICT, "金额超过支持范围");
        }
        source.setBalanceCent(from.getBalanceCent());
        target.setBalanceCent(to.getBalanceCent());
        target.setFrozenBalanceCent(to.getFrozenBalanceCent());
        if (wallets.updateById(source) != 1 || wallets.updateById(target) != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, "钱包状态已变化");
        }
        WalletTransaction tx = new WalletTransaction();
        tx.setTransactionNo(UUID.randomUUID().toString());
        tx.setTransactionType("PAYMENT");
        tx.setRequestWalletId(sourceId);
        tx.setRequestKey(requestId);
        tx.setSourceWalletId(sourceId);
        tx.setDestinationWalletId(targetId);
        tx.setAmountCent(amountCent);
        tx.setRewardCent(0L);
        tx.setFeeCent(0L);
        tx.setSourceBalanceAfterCent(source.getBalanceCent());
        tx.setDestinationBalanceAfterCent(target.getBalanceCent());
        transactions.insert(tx);
        WalletPayment state = new WalletPayment();
        state.setTransactionId(tx.getId());
        state.setSettlementStatus("FROZEN");
        payments.insert(state);
        return view(tx, state);
    }

    @Transactional(readOnly = true)
    public PaymentView get(String ownerType, long ownerId, long transactionId) {
        long viewer = accounts.get(ownerType, ownerId).id();
        WalletTransaction tx = requireTransaction(transactionId);
        if (!Long.valueOf(viewer).equals(tx.getSourceWalletId()) && !Long.valueOf(viewer).equals(tx.getDestinationWalletId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "无权查看该付款");
        }
        return view(tx, requirePaymentState(transactionId));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PaymentView confirm(String ownerType, long ownerId, long transactionId) {
        long viewer = accounts.get(ownerType, ownerId).id();
        WalletTransaction tx = requireTransaction(transactionId);
        if (!Long.valueOf(viewer).equals(tx.getSourceWalletId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "只有付款方可以确认");
        }
        WalletEntity target = wallets.lockById(tx.getDestinationWalletId());
        if (target == null) throw new BusinessException(HttpStatus.NOT_FOUND, "收款钱包不存在");
        WalletPayment state = payments.lockById(transactionId);
        if (state == null) throw new BusinessException(HttpStatus.CONFLICT, "付款状态不存在");
        if ("CONFIRMED".equals(state.getSettlementStatus())) return view(tx, state);
        if (!"FROZEN".equals(state.getSettlementStatus())) throw new BusinessException(HttpStatus.CONFLICT, "付款状态无效");
        VirtualWallet to = new VirtualWallet(target.getBalanceCent(), target.getFrozenBalanceCent());
        try {
            to.unfreeze(tx.getAmountCent());
        } catch (IllegalStateException exception) {
            throw new BusinessException(HttpStatus.CONFLICT, "冻结金额不足");
        }
        target.setFrozenBalanceCent(to.getFrozenBalanceCent());
        if (wallets.updateById(target) != 1) throw new BusinessException(HttpStatus.CONFLICT, "钱包状态已变化");
        state.setSettlementStatus("CONFIRMED");
        state.setConfirmedAt(LocalDateTime.now());
        if (payments.updateById(state) != 1) throw new BusinessException(HttpStatus.CONFLICT, "付款状态已变化");
        return view(tx, state);
    }

    private WalletTransaction requireTransaction(long id) {
        WalletTransaction tx = transactions.selectById(id);
        if (tx == null || !"PAYMENT".equals(tx.getTransactionType())) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "付款记录不存在");
        }
        return tx;
    }
    private WalletPayment requirePaymentState(long id) {
        WalletPayment state = payments.selectById(id);
        if (state == null) throw new BusinessException(HttpStatus.CONFLICT, "付款状态不存在");
        return state;
    }
    private PaymentView view(WalletTransaction tx, WalletPayment state) {
        return new PaymentView(tx.getId(), tx.getSourceWalletId(), tx.getDestinationWalletId(),
                tx.getAmountCent(), state.getSettlementStatus());
    }
    public record PaymentView(long transactionId, long sourceWalletId, long destinationWalletId,
                              long amountCent, String status) {}
}
