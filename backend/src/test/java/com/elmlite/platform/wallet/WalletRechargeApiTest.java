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

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:wallet_recharge_api;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;LOCK_TIMEOUT=10000")
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Sql(scripts = {"/db/h2/admin-schema.sql", "/db/h2/wallet-recharge-schema.sql"})
class WalletRechargeApiTest {
    @Autowired private MockMvc mvc;
    @Autowired private JwtTokenService tokens;
    @Autowired private ObjectMapper json;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void rechargeRecordsPrincipalAndBonus() throws Exception {
        var result = recharge(1, AccountType.USER, 100_000L, "charge_1");
        assertEquals(200, result.getResponse().getStatus());
        var data = json.readTree(result.getResponse().getContentAsString()).path("data");
        assertEquals(100_000L, data.path("amountCent").asLong());
        assertEquals(10_000L, data.path("rewardCent").asLong());
        assertEquals(110_000L, data.path("balanceAfterCent").asLong());
        assertEquals(110_000L, balance(1));
        assertEquals(1, transactionCount());
        mvc.perform(get("/api/v1/wallets/me/transactions").header("Authorization", bearer(1, AccountType.USER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].transactionType").value("RECHARGE"))
                .andExpect(jsonPath("$.data[0].sourceWalletId").value(nullValue()))
                .andExpect(jsonPath("$.data[0].destinationWalletId").value(1))
                .andExpect(jsonPath("$.data[0].rewardCent").value(10_000));
    }

    @Test
    void replayIsIdempotentEvenAfterRuleChanges() throws Exception {
        var first = recharge(1, AccountType.USER, 100_000L, "same_key");
        assertEquals(200, first.getResponse().getStatus());
        long id = json.readTree(first.getResponse().getContentAsString()).path("data").path("transactionId").asLong();
        updateRule(50_000L, 20_000L, true).andExpect(status().isOk());
        var replay = recharge(1, AccountType.USER, 100_000L, "same_key");
        assertEquals(200, replay.getResponse().getStatus());
        var data = json.readTree(replay.getResponse().getContentAsString()).path("data");
        assertEquals(id, data.path("transactionId").asLong());
        assertEquals(10_000L, data.path("rewardCent").asLong());
        assertEquals(110_000L, data.path("balanceAfterCent").asLong());
        assertEquals(110_000L, balance(1));
        assertEquals(1, transactionCount());
        assertEquals(409, recharge(1, AccountType.USER, 100_001L, "same_key").getResponse().getStatus());
        assertEquals(1, transactionCount());
    }

    @Test
    void administratorCanChangeAndDisableRewardRule() throws Exception {
        mvc.perform(get("/api/v1/admin/wallet/recharge-rule").header("Authorization", bearer(1, AccountType.ADMIN)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.thresholdCent").value(100_000));
        updateRule(500L, 20L, true).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rewardCent").value(20));
        assertEquals(200, recharge(1, AccountType.USER, 500L, "custom_rule").getResponse().getStatus());
        assertEquals(520L, balance(1));
        updateRule(500L, 20L, false).andExpect(status().isOk());
        assertEquals(200, recharge(1, AccountType.USER, 500L, "disabled_rule").getResponse().getStatus());
        assertEquals(1_020L, balance(1));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"amountCent\":0,\"requestId\":\"a\"}",
            "{\"amountCent\":-1,\"requestId\":\"a\"}",
            "{\"amountCent\":1.5,\"requestId\":\"a\"}",
            "{\"amountCent\":10000000000,\"requestId\":\"a\"}",
            "{\"amountCent\":1,\"requestId\":\"\"}",
            "{\"amountCent\":1,\"requestId\":\"bad space\"}",
            "{\"amountCent\":1}"
    })
    void rejectsInvalidRechargeInput(String body) throws Exception {
        mvc.perform(post("/api/v1/wallets/me/recharges").header("Authorization", bearer(1, AccountType.USER))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        assertEquals(0L, balance(1));
        assertEquals(0, transactionCount());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"thresholdCent\":0,\"rewardCent\":0,\"enabled\":true}",
            "{\"thresholdCent\":1,\"rewardCent\":-1,\"enabled\":true}",
            "{\"thresholdCent\":1,\"rewardCent\":10000000000,\"enabled\":true}",
            "{\"thresholdCent\":1,\"rewardCent\":1}"
    })
    void rejectsInvalidRuleInput(String body) throws Exception {
        mvc.perform(put("/api/v1/admin/wallet/recharge-rule").header("Authorization", bearer(1, AccountType.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rolesAndDisabledAccountsAreChecked() throws Exception {
        mvc.perform(post("/api/v1/wallets/me/recharges").contentType(MediaType.APPLICATION_JSON)
                        .content(body(1L, "anon"))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/wallets/me/transactions")).andExpect(status().isUnauthorized());
        for (AccountType type : List.of(AccountType.RIDER, AccountType.ADMIN)) {
            mvc.perform(post("/api/v1/wallets/me/recharges").header("Authorization", bearer(1, type))
                            .contentType(MediaType.APPLICATION_JSON).content(body(1L, "forbidden")))
                    .andExpect(status().isForbidden());
        }
        for (AccountType type : List.of(AccountType.USER, AccountType.MERCHANT)) {
            mvc.perform(get("/api/v1/admin/wallet/recharge-rule").header("Authorization", bearer(1, type)))
                    .andExpect(status().isForbidden());
        }
        jdbc.update("UPDATE users SET status=0 WHERE id=1");
        assertEquals(403, recharge(1, AccountType.USER, 1L, "disabled_user").getResponse().getStatus());
        jdbc.update("UPDATE merchant SET status=0 WHERE id=1");
        assertEquals(403, recharge(1, AccountType.MERCHANT, 1L, "disabled_merchant").getResponse().getStatus());
        jdbc.update("UPDATE admin_account SET status=0 WHERE id=1");
        mvc.perform(get("/api/v1/admin/wallet/recharge-rule").header("Authorization", bearer(1, AccountType.ADMIN)))
                .andExpect(status().isForbidden());
    }

    @Test
    void eachOwnerCanSeeOnlyTheirOwnTransactions() throws Exception {
        assertEquals(200, recharge(1, AccountType.USER, 100L, "visible").getResponse().getStatus());
        for (var owner : List.of(Map.entry(2L, AccountType.USER), Map.entry(1L, AccountType.MERCHANT))) {
            mvc.perform(get("/api/v1/wallets/me/transactions").header("Authorization", bearer(owner.getKey(), owner.getValue())))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data", hasSize(0)));
        }
    }

    @Test
    void merchantAndUserWithSameIdHaveSeparateWallets() throws Exception {
        assertEquals(200, recharge(1, AccountType.MERCHANT, 300L, "merchant_charge").getResponse().getStatus());
        assertEquals(0L, balance(1));
        assertEquals(300L, balance(3));
        mvc.perform(get("/api/v1/wallets/me/transactions").header("Authorization", bearer(1, AccountType.MERCHANT)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].destinationWalletId").value(3));
        mvc.perform(get("/api/v1/wallets/me/transactions").header("Authorization", bearer(1, AccountType.USER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void missingWalletOrRuleIsReported() throws Exception {
        jdbc.update("DELETE FROM virtual_wallet WHERE id=2");
        assertEquals(404, recharge(2, AccountType.USER, 1L, "missing_wallet").getResponse().getStatus());
        mvc.perform(get("/api/v1/wallets/me/transactions").header("Authorization", bearer(2, AccountType.USER)))
                .andExpect(status().isNotFound());
        jdbc.update("DELETE FROM wallet_recharge_rule WHERE id=1");
        assertEquals(409, recharge(1, AccountType.USER, 1L, "missing_rule").getResponse().getStatus());
        mvc.perform(get("/api/v1/admin/wallet/recharge-rule").header("Authorization", bearer(1, AccountType.ADMIN)))
                .andExpect(status().isConflict());
        assertEquals(0L, balance(1));
    }

    @Test
    void overflowDoesNotChangeBalanceOrLedger() throws Exception {
        jdbc.update("UPDATE virtual_wallet SET balance_cent=? WHERE id=1", Long.MAX_VALUE - 10L);
        assertEquals(409, recharge(1, AccountType.USER, 20L, "overflow").getResponse().getStatus());
        assertEquals(Long.MAX_VALUE - 10L, balance(1));
        assertEquals(0, transactionCount());
    }

    @Test
    void ledgerInsertFailureRollsBackBalance() throws Exception {
        assertEquals(500, recharge(1, AccountType.USER, 100L, "FAIL_LEDGER").getResponse().getStatus());
        assertEquals(0L, balance(1));
        assertEquals(0, transactionCount());
    }

    @Test
    void simultaneousRepeatedRequestIsAppliedOnce() throws Exception {
        String authorization = bearer(1, AccountType.USER);
        String content = body(100_000L, "parallel_same");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Integer> action = () -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("start timeout");
                return mvc.perform(post("/api/v1/wallets/me/recharges").header("Authorization", authorization)
                                .contentType(MediaType.APPLICATION_JSON).content(content))
                        .andReturn().getResponse().getStatus();
            };
            Future<Integer> first = executor.submit(action);
            Future<Integer> second = executor.submit(action);
            if (!ready.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("ready timeout");
            start.countDown();
            assertEquals(List.of(200, 200), java.util.stream.Stream.of(
                    first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS)).sorted().toList());
        }
        assertEquals(110_000L, balance(1));
        assertEquals(1, transactionCount());
    }

    @Test
    void concurrentDistinctRequestsBothUpdateBalance() throws Exception {
        String authorization = bearer(1, AccountType.USER);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            java.util.function.Function<String, java.util.concurrent.Callable<Integer>> action = key -> () -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("start timeout");
                return mvc.perform(post("/api/v1/wallets/me/recharges").header("Authorization", authorization)
                                .contentType(MediaType.APPLICATION_JSON).content(body(600L, key)))
                        .andReturn().getResponse().getStatus();
            };
            Future<Integer> first = executor.submit(action.apply("parallel_1"));
            Future<Integer> second = executor.submit(action.apply("parallel_2"));
            if (!ready.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("ready timeout");
            start.countDown();
            assertEquals(List.of(200, 200), java.util.stream.Stream.of(
                    first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS)).sorted().toList());
        }
        assertEquals(1_200L, balance(1));
        assertEquals(2, transactionCount());
    }

    private org.springframework.test.web.servlet.MvcResult recharge(long id, AccountType type, long amount, String key) throws Exception {
        return mvc.perform(post("/api/v1/wallets/me/recharges").header("Authorization", bearer(id, type))
                .contentType(MediaType.APPLICATION_JSON).content(body(amount, key))).andReturn();
    }

    private org.springframework.test.web.servlet.ResultActions updateRule(long threshold, long reward, boolean enabled) throws Exception {
        return mvc.perform(put("/api/v1/admin/wallet/recharge-rule").header("Authorization", bearer(1, AccountType.ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                        "thresholdCent", threshold, "rewardCent", reward, "enabled", enabled))));
    }

    private String body(long amount, String key) throws Exception {
        return json.writeValueAsString(Map.of("amountCent", amount, "requestId", key));
    }

    private String bearer(long id, AccountType type) {
        return "Bearer " + tokens.issue(id, type);
    }

    private long balance(long id) {
        return jdbc.queryForObject("SELECT balance_cent FROM virtual_wallet WHERE id=?", Long.class, id);
    }

    private int transactionCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM wallet_transaction", Integer.class);
    }
}
