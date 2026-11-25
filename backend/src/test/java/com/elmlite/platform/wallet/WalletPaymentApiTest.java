package com.elmlite.platform.wallet;

import com.elmlite.platform.service.JwtTokenService;
import com.elmlite.platform.service.JwtTokenService.AccountType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:wallet_payment_api;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;LOCK_TIMEOUT=10000")
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Sql(scripts = {"/db/h2/admin-schema.sql", "/db/h2/wallet-payment-reset.sql", "/db/h2/wallet-recharge-schema.sql", "/db/h2/wallet-payment-schema.sql"})
class WalletPaymentApiTest {
    @Autowired MockMvc mvc;
    @Autowired JwtTokenService tokens;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    @Test
    void paymentMovesMoneyAndFreezesRecipientUntilPayerConfirms() throws Exception {
        MvcResult payment = pay(1, AccountType.USER, "MERCHANT", 1L, 1_000L, "payment_1");
        assertEquals(200, payment.getResponse().getStatus());
        long id = transactionId(payment);
        assertEquals("FROZEN", data(payment).path("status").asText());
        assertWallet(1, 4_000, 2_000);
        assertWallet(3, 1_500, 1_100);
        assertEquals(1, count());
        mvc.perform(get("/api/v1/wallets/me/transactions").header("Authorization", auth(1, AccountType.USER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].transactionType").value("PAYMENT"))
                .andExpect(jsonPath("$.data[0].sourceWalletId").value(1))
                .andExpect(jsonPath("$.data[0].destinationWalletId").value(3));
        mvc.perform(get("/api/v1/wallets/me/transactions").header("Authorization", auth(1, AccountType.MERCHANT)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data", hasSize(1)));
        mvc.perform(get("/api/v1/wallets/me/payments/{id}", id).header("Authorization", auth(1, AccountType.MERCHANT)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("FROZEN"));
        assertEquals(403, confirm(1, AccountType.MERCHANT, id).getResponse().getStatus());
        assertWallet(3, 1_500, 1_100);
        assertEquals(200, confirm(1, AccountType.USER, id).getResponse().getStatus());
        assertWallet(3, 1_500, 100);
        assertEquals(200, confirm(1, AccountType.USER, id).getResponse().getStatus());
        assertWallet(3, 1_500, 100);
        mvc.perform(get("/api/v1/wallets/me/payments/{id}", id).header("Authorization", auth(1, AccountType.USER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("CONFIRMED"));
    }

    @Test
    void retriesAreIdempotentAndCannotChangeAnExistingRequest() throws Exception {
        MvcResult first = pay(1, AccountType.USER, "MERCHANT", 1L, 500L, "same_key");
        assertEquals(200, first.getResponse().getStatus());
        long id = transactionId(first);
        MvcResult retry = pay(1, AccountType.USER, "MERCHANT", 1L, 500L, "same_key");
        assertEquals(200, retry.getResponse().getStatus());
        assertEquals(id, transactionId(retry));
        assertEquals(409, pay(1, AccountType.USER, "MERCHANT", 1L, 501L, "same_key").getResponse().getStatus());
        assertEquals(409, pay(1, AccountType.USER, "USER", 2L, 500L, "same_key").getResponse().getStatus());
        assertEquals(200, confirm(1, AccountType.USER, id).getResponse().getStatus());
        assertEquals("CONFIRMED", data(pay(1, AccountType.USER, "MERCHANT", 1L, 500L, "same_key")).path("status").asText());
        assertWallet(1, 4_500, 2_000);
        assertWallet(3, 1_000, 100);
        assertEquals(1, count());
    }

    @Test
    void existingRechargeRequestKeyCannotBeUsedForPayment() throws Exception {
        mvc.perform(post("/api/v1/wallets/me/recharges").header("Authorization", auth(1, AccountType.USER))
                .contentType(MediaType.APPLICATION_JSON).content("{\"amountCent\":1,\"requestId\":\"used\"}"))
                .andExpect(status().isOk());
        assertEquals(409, pay(1, AccountType.USER, "MERCHANT", 1L, 1L, "used").getResponse().getStatus());
        assertWallet(1, 5_001, 2_000);
        assertWallet(3, 500, 100);
        assertEquals(1, count());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{\"recipientType\":\"MERCHANT\",\"recipientId\":1,\"amountCent\":0,\"requestId\":\"a\"}",
        "{\"recipientType\":\"MERCHANT\",\"recipientId\":1,\"amountCent\":1.5,\"requestId\":\"a\"}",
        "{\"recipientType\":\"MERCHANT\",\"recipientId\":1,\"amountCent\":10000000000,\"requestId\":\"a\"}",
        "{\"recipientType\":\"RIDER\",\"recipientId\":1,\"amountCent\":1,\"requestId\":\"a\"}",
        "{\"recipientType\":\"MERCHANT\",\"recipientId\":0,\"amountCent\":1,\"requestId\":\"a\"}",
        "{\"recipientType\":\"MERCHANT\",\"recipientId\":1,\"amountCent\":1,\"requestId\":\"bad key\"}"
    })
    void invalidInputsDoNotChangeWallets(String body) throws Exception {
        mvc.perform(post("/api/v1/wallets/me/payments").header("Authorization", auth(1, AccountType.USER))
                        .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        assertWallet(1, 5_000, 2_000);
        assertWallet(3, 500, 100);
        assertEquals(0, count());
    }

    @Test
    void rejectsInsufficientAvailableFundsMissingOrDisabledRecipientsAndSelfPayment() throws Exception {
        assertEquals(409, pay(1, AccountType.USER, "MERCHANT", 1L, 3_001L, "too_much").getResponse().getStatus());
        assertEquals(400, pay(1, AccountType.USER, "USER", 1L, 1L, "self").getResponse().getStatus());
        assertEquals(404, pay(1, AccountType.USER, "USER", 99L, 1L, "missing").getResponse().getStatus());
        jdbc.update("UPDATE merchant SET status=0 WHERE id=1");
        assertEquals(403, pay(1, AccountType.USER, "MERCHANT", 1L, 1L, "disabled").getResponse().getStatus());
        assertWallet(1, 5_000, 2_000);
        assertWallet(3, 500, 100);
        assertEquals(0, count());
    }

    @Test
    void validatesRolesOwnershipAndMissingPayment() throws Exception {
        mvc.perform(post("/api/v1/wallets/me/payments").contentType(MediaType.APPLICATION_JSON)
                .content(body("MERCHANT",1,1,"no_auth"))).andExpect(status().isUnauthorized());
        assertEquals(403, pay(1, AccountType.RIDER, "MERCHANT", 1L, 1L, "rider").getResponse().getStatus());
        MvcResult payment = pay(1, AccountType.USER, "MERCHANT", 1L, 100L, "owned");
        assertEquals(200, payment.getResponse().getStatus());
        long id = transactionId(payment);
        assertEquals(403, confirm(2, AccountType.USER, id).getResponse().getStatus());
        mvc.perform(get("/api/v1/wallets/me/payments/{id}", id).header("Authorization", auth(2, AccountType.USER)))
                .andExpect(status().isForbidden());
        assertEquals(404, confirm(1, AccountType.USER, 999999L).getResponse().getStatus());
    }

    @Test
    void overflowOrLedgerFailureRollsBackBothWallets() throws Exception {
        jdbc.update("UPDATE virtual_wallet SET balance_cent=? WHERE id=3", Long.MAX_VALUE - 10L);
        assertEquals(409, pay(1, AccountType.USER, "MERCHANT", 1L, 20L, "overflow").getResponse().getStatus());
        assertWallet(1, 5_000, 2_000);
        assertWallet(3, Long.MAX_VALUE - 10L, 100);
        jdbc.update("UPDATE virtual_wallet SET balance_cent=500 WHERE id=3");
        assertEquals(500, pay(1, AccountType.USER, "MERCHANT", 1L, 100L, "FAIL_LEDGER").getResponse().getStatus());
        assertWallet(1, 5_000, 2_000);
        assertWallet(3, 500, 100);
        assertEquals(0, count());
    }

    @Test
    void paymentStatusInsertFailureRollsBackLedgerAndBothWallets() throws Exception {
        jdbc.execute("ALTER TABLE wallet_payment ADD CONSTRAINT force_test_failure CHECK (settlement_status <> 'FROZEN')");
        assertEquals(500, pay(1, AccountType.USER, "MERCHANT", 1L, 100L, "fail_status").getResponse().getStatus());
        assertWallet(1, 5_000, 2_000);
        assertWallet(3, 500, 100);
        assertEquals(0, count());
    }

    @Test
    void confirmationsReleaseOnlyTheirOwnPayments() throws Exception {
        var first = pay(1, AccountType.USER, "MERCHANT", 1L, 100L, "separate_a");
        var second = pay(1, AccountType.USER, "MERCHANT", 1L, 200L, "separate_b");
        assertEquals(200, first.getResponse().getStatus());
        assertEquals(200, second.getResponse().getStatus());
        assertWallet(3, 800, 400);
        assertEquals(200, confirm(1, AccountType.USER, transactionId(first)).getResponse().getStatus());
        assertWallet(3, 800, 300);
        assertEquals(200, confirm(1, AccountType.USER, transactionId(second)).getResponse().getStatus());
        assertWallet(3, 800, 100);
    }

    @Test
    void parallelRetriesAndConfirmationsHaveExactlyOneEffect() throws Exception {
        var results = concurrently(
                () -> pay(1, AccountType.USER, "MERCHANT", 1L, 100L, "parallel"),
                () -> pay(1, AccountType.USER, "MERCHANT", 1L, 100L, "parallel"));
        assertEquals(200, results.get(0).getResponse().getStatus());
        assertEquals(200, results.get(1).getResponse().getStatus());
        long id = transactionId(results.get(0));
        assertEquals(id, transactionId(results.get(1)));
        assertEquals(1, count());
        results = concurrently(() -> confirm(1, AccountType.USER, id), () -> confirm(1, AccountType.USER, id));
        assertEquals(200, results.get(0).getResponse().getStatus());
        assertEquals(200, results.get(1).getResponse().getStatus());
        assertWallet(1, 4_900, 2_000);
        assertWallet(3, 600, 100);
    }

    @Test
    void oppositeDirectionPaymentsDoNotDeadlock() throws Exception {
        var results = concurrently(
                () -> pay(1, AccountType.USER, "MERCHANT", 1L, 100L, "out"),
                () -> pay(1, AccountType.MERCHANT, "USER", 1L, 100L, "back"));
        assertEquals(200, results.get(0).getResponse().getStatus());
        assertEquals(200, results.get(1).getResponse().getStatus());
        assertWallet(1, 5_000, 2_100);
        assertWallet(3, 500, 200);
        assertEquals(2, count());
    }

    private List<MvcResult> concurrently(java.util.concurrent.Callable<MvcResult> one,
                                         java.util.concurrent.Callable<MvcResult> two) throws Exception {
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            java.util.function.Function<java.util.concurrent.Callable<MvcResult>,java.util.concurrent.Callable<MvcResult>> wrap = action -> () -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("start timeout");
                return action.call();
            };
            var a = executor.submit(wrap.apply(one));
            var b = executor.submit(wrap.apply(two));
            if (!ready.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("ready timeout");
            start.countDown();
            return List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
        }
    }
    private MvcResult pay(long id, AccountType type, String toType, long toId, long amount, String key) throws Exception {
        return mvc.perform(post("/api/v1/wallets/me/payments").header("Authorization", auth(id,type))
                .contentType(MediaType.APPLICATION_JSON).content(body(toType,toId,amount,key))).andReturn();
    }
    private MvcResult confirm(long id, AccountType type, long paymentId) throws Exception {
        return mvc.perform(post("/api/v1/wallets/me/payments/{id}/confirm", paymentId)
                .header("Authorization", auth(id,type))).andReturn();
    }
    private String body(String type, long id, long amount, String key) throws Exception {
        return json.writeValueAsString(Map.of("recipientType",type,"recipientId",id,"amountCent",amount,"requestId",key));
    }
    private String auth(long id, AccountType type) { return "Bearer " + tokens.issue(id,type); }
    private com.fasterxml.jackson.databind.JsonNode data(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString()).path("data");
    }
    private long transactionId(MvcResult result) throws Exception { return data(result).path("transactionId").asLong(); }
    private int count() { return jdbc.queryForObject("SELECT COUNT(*) FROM wallet_transaction", Integer.class); }
    private void assertWallet(long id, long balance, long frozen) {
        Map<String,Object> row = jdbc.queryForMap("SELECT balance_cent, frozen_balance_cent FROM virtual_wallet WHERE id=?",id);
        assertEquals(balance, ((Number) row.get("balance_cent")).longValue());
        assertEquals(frozen, ((Number) row.get("frozen_balance_cent")).longValue());
    }
}
