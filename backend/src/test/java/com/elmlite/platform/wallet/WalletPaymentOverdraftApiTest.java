package com.elmlite.platform.wallet;

import com.elmlite.platform.service.JwtTokenService;
import com.elmlite.platform.service.JwtTokenService.AccountType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:wallet_payment_overdraft_api;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;LOCK_TIMEOUT=10000")
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Sql(scripts = {
        "/db/h2/admin-schema.sql",
        "/db/h2/wallet-payment-reset.sql",
        "/db/h2/wallet-credit-reset.sql",
        "/db/h2/wallet-recharge-schema.sql",
        "/db/h2/wallet-credit-schema.sql",
        "/db/h2/wallet-payment-schema.sql"
})
class WalletPaymentOverdraftApiTest {

    @Autowired private MockMvc mvc;
    @Autowired private JwtTokenService tokens;
    @Autowired private ObjectMapper json;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void optedInPaymentBorrowsOnlyShortfallAndCanBeRetried() throws Exception {
        configure(10_000L);

        MvcResult payment = pay(
                1, AccountType.USER, "MERCHANT", 1L, 5000L, "auto_pay", true);

        assertEquals(200, payment.getResponse().getStatus());
        assertEquals("FROZEN", data(payment).path("status").asText());
        assertEquals(2000L, principal());
        assertEquals(2000L, balance(1));
        assertEquals(2000L, frozen(1));
        assertEquals(5500L, balance(3));
        assertEquals(5100L, frozen(3));
        assertEquals(2, transactionCount());

        MvcResult retry = pay(
                1, AccountType.USER, "MERCHANT", 1L, 5000L, "auto_pay", true);

        assertEquals(200, retry.getResponse().getStatus());
        assertEquals(data(payment).path("transactionId").asLong(),
                data(retry).path("transactionId").asLong());
        assertEquals(2000L, principal());
        assertEquals(2, transactionCount());

        long transactionId = data(payment).path("transactionId").asLong();

        mvc.perform(post("/api/v1/wallets/me/payments/{id}/confirm", transactionId)
                        .header("Authorization", bearer(1, AccountType.USER)))
                .andExpect(status().isOk());

        assertEquals(100L, frozen(3));
    }

    @Test
    void paymentDoesNotBorrowWithoutExplicitConsent() throws Exception {
        configure(10_000L);

        MvcResult result = pay(
                1, AccountType.USER, "MERCHANT", 1L, 5000L, "no_credit", false);

        assertEquals(409, result.getResponse().getStatus());
        assertEquals(0L, principal());
        assertEquals(5000L, balance(1));
        assertEquals(500L, balance(3));
        assertEquals(0, transactionCount());
    }

    @Test
    void sufficientBalanceDoesNotRequireCreditAccount() throws Exception {
        MvcResult result = pay(
                1, AccountType.USER, "MERCHANT", 1L, 2000L, "enough", true);

        assertEquals(200, result.getResponse().getStatus());
        assertEquals(0, creditCount());
        assertEquals(1, transactionCount());
        assertEquals(3000L, balance(1));
    }

    @Test
    void missingOrInsufficientCreditDoesNotChangeWallets() throws Exception {
        MvcResult missing = pay(
                1, AccountType.USER, "MERCHANT", 1L, 5000L, "missing", true);

        assertEquals(409, missing.getResponse().getStatus());
        assertEquals(5000L, balance(1));
        assertEquals(0, transactionCount());

        configure(1999L);

        MvcResult insufficient = pay(
                1, AccountType.USER, "MERCHANT", 1L, 5000L, "too_small", true);

        assertEquals(409, insufficient.getResponse().getStatus());
        assertEquals(0L, principal());
        assertEquals(5000L, balance(1));
        assertEquals(500L, balance(3));
        assertEquals(0, transactionCount());
    }

    @Test
    void paymentLedgerFailureRollsBackAutomaticBorrow() throws Exception {
        configure(10_000L);

        MvcResult result = pay(
                1, AccountType.USER, "MERCHANT", 1L, 5000L, "FAIL_LEDGER", true);

        assertEquals(500, result.getResponse().getStatus());
        assertEquals(0L, principal());
        assertEquals(5000L, balance(1));
        assertEquals(2000L, frozen(1));
        assertEquals(500L, balance(3));
        assertEquals(100L, frozen(3));
        assertEquals(0, transactionCount());
    }

    @Test
    void recipientOverflowRollsBackAutomaticBorrow() throws Exception {
        configure(10_000L);

        jdbc.update(
                "UPDATE virtual_wallet SET balance_cent=? WHERE id=3",
                Long.MAX_VALUE - 10L);

        MvcResult result = pay(
                1, AccountType.USER, "MERCHANT", 1L, 5000L, "overflow", true);

        assertEquals(409, result.getResponse().getStatus());
        assertEquals(0L, principal());
        assertEquals(5000L, balance(1));
        assertEquals(Long.MAX_VALUE - 10L, balance(3));
        assertEquals(0, transactionCount());
    }

    @Test
    void merchantCannotUseUserCredit() throws Exception {
        configure(10_000L);

        MvcResult result = pay(
                1, AccountType.MERCHANT, "USER", 1L, 500L, "merchant_pay", true);

        assertEquals(409, result.getResponse().getStatus());
        assertEquals(0L, principal());
        assertEquals(0, transactionCount());
    }

    @Test
    void concurrentRetriesBorrowOnlyOnce() throws Exception {
        configure(10_000L);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<MvcResult> action = () -> {
                ready.countDown();

                if (!start.await(20, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("start timeout");
                }

                return pay(
                        1, AccountType.USER, "MERCHANT", 1L,
                        5000L, "parallel_auto", true);
            };

            var first = executor.submit(action);
            var second = executor.submit(action);

            if (!ready.await(20, TimeUnit.SECONDS)) {
                throw new IllegalStateException("ready timeout");
            }

            start.countDown();

            MvcResult one = first.get(30, TimeUnit.SECONDS);
            MvcResult two = second.get(30, TimeUnit.SECONDS);

            assertEquals(200, one.getResponse().getStatus());
            assertEquals(200, two.getResponse().getStatus());
            assertEquals(data(one).path("transactionId").asLong(),
                    data(two).path("transactionId").asLong());
        }

        assertEquals(2000L, principal());
        assertEquals(2000L, balance(1));
        assertEquals(2, transactionCount());
    }

    @Test
    void automaticBorrowCanBeRepaidAfterRecharge() throws Exception {
        configure(10_000L);

        assertEquals(200, pay(
                1, AccountType.USER, "MERCHANT", 1L,
                5000L, "pay_then_repay", true).getResponse().getStatus());

        mvc.perform(post("/api/v1/wallets/me/recharges")
                        .header("Authorization", bearer(1, AccountType.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "amountCent", 2000L,
                                "requestId", "repayment_funds"))))
                .andExpect(status().isOk());

        mvc.perform(post("/api/v1/wallets/me/credit/repayments")
                        .header("Authorization", bearer(1, AccountType.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "amountCent", 2000L,
                                "requestId", "repay_auto"))))
                .andExpect(status().isOk());

        assertEquals(0L, principal());
        assertEquals(2000L, balance(1));
        assertEquals(2000L, frozen(1));
        assertEquals(4, transactionCount());
    }

    private void configure(long limit) throws Exception {
        mvc.perform(put("/api/v1/admin/wallets/1/credit")
                        .header("Authorization", bearer(1, AccountType.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "creditLimitCent", limit,
                                "repaymentDays", 7,
                                "dailyRateBasisPoints", 5))))
                .andExpect(status().isOk());
    }

    private MvcResult pay(
            long id, AccountType type, String recipientType, long recipientId,
            long amount, String requestId, boolean useCredit) throws Exception {

        return mvc.perform(post("/api/v1/wallets/me/payments")
                        .header("Authorization", bearer(id, type))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "recipientType", recipientType,
                                "recipientId", recipientId,
                                "amountCent", amount,
                                "requestId", requestId,
                                "useCredit", useCredit))))
                .andReturn();
    }

    private String bearer(long id, AccountType type) {
        return "Bearer " + tokens.issue(id, type);
    }

    private com.fasterxml.jackson.databind.JsonNode data(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString()).path("data");
    }

    private long balance(long id) {
        return jdbc.queryForObject(
                "SELECT balance_cent FROM virtual_wallet WHERE id=?",
                Long.class, id);
    }

    private long frozen(long id) {
        return jdbc.queryForObject(
                "SELECT frozen_balance_cent FROM virtual_wallet WHERE id=?",
                Long.class, id);
    }

    private long principal() {
        return jdbc.queryForObject(
                "SELECT principal_cent FROM wallet_credit_account WHERE wallet_id=1",
                Long.class);
    }

    private int creditCount() {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM wallet_credit_account", Integer.class);
    }

    private int transactionCount() {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM wallet_transaction", Integer.class);
    }
}
