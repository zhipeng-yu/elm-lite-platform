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

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:wallet_withdrawal_api;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;LOCK_TIMEOUT=10000")
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Sql(scripts = {"/db/h2/admin-schema.sql", "/db/h2/wallet-credit-reset.sql", "/db/h2/wallet-recharge-schema.sql", "/db/h2/wallet-credit-schema.sql", "/db/h2/wallet-withdrawal-rule-schema.sql"})
class WalletWithdrawalApiTest {

    @Autowired private MockMvc mvc;
    @Autowired private JwtTokenService tokens;
    @Autowired private ObjectMapper json;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void withdrawalForfeitsRewardAndIsIdempotent() throws Exception {
        assertEquals(200, recharge(100_000L, "charge_one").getResponse().getStatus());

        MvcResult first = withdraw(1, AccountType.USER, 110_000L, "withdraw_one");
        assertEquals(200, first.getResponse().getStatus());

        var data = data(first);
        assertEquals(110_000L, data.path("amountCent").asLong());
        assertEquals(11_000L, data.path("feeCent").asLong());
        assertEquals(99_000L, data.path("netCent").asLong());
        assertEquals(10_000L, data.path("rewardForfeitedCent").asLong());
        assertEquals(0L, data.path("balanceAfterCent").asLong());
        assertEquals(0L, balance(1));

        MvcResult retry = withdraw(1, AccountType.USER, 110_000L, "withdraw_one");
        assertEquals(200, retry.getResponse().getStatus());
        assertEquals(data.path("transactionId").asLong(),
                data(retry).path("transactionId").asLong());

        assertEquals(409,
                withdraw(1, AccountType.USER, 110_001L, "withdraw_one")
                        .getResponse().getStatus());

        assertEquals(2, transactionCount());
    }

    @Test
    void rewardAbovePercentageFeeIsForfeited() throws Exception {
        mvc.perform(put("/api/v1/admin/wallet/recharge-rule")
                        .header("Authorization", bearer(1, AccountType.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"thresholdCent\":10000,\"rewardCent\":20000,\"enabled\":true}"))
                .andExpect(status().isOk());

        assertEquals(200, recharge(10_000L, "large_reward").getResponse().getStatus());

        MvcResult result = withdraw(1, AccountType.USER, 25_000L, "take_out");
        assertEquals(200, result.getResponse().getStatus());

        var data = data(result);
        assertEquals(20_000L, data.path("feeCent").asLong());
        assertEquals(5_000L, data.path("netCent").asLong());
        assertEquals(20_000L, data.path("rewardForfeitedCent").asLong());
        assertEquals(5_000L, balance(1));
    }

    @Test
    void historyIsProcessedInOrderAndRewardIsNotDeductedTwice() throws Exception {
        assertEquals(200, recharge(100_000L, "first_charge").getResponse().getStatus());

        jdbc.update("UPDATE virtual_wallet SET balance_cent=balance_cent-3000 WHERE id=1");
        jdbc.update("UPDATE virtual_wallet SET balance_cent=3000, frozen_balance_cent=3000 WHERE id=3");

        jdbc.update(
                "INSERT INTO wallet_transaction " +
                "(transaction_no, transaction_type, request_wallet_id, request_key, " +
                "source_wallet_id, destination_wallet_id, amount_cent, reward_cent, fee_cent, " +
                "source_balance_after_cent, destination_balance_after_cent) " +
                "VALUES ('historic-payment', 'PAYMENT', 1, 'historic', 1, 3, " +
                "3000, 0, 0, 107000, 3000)");

        assertEquals(200, recharge(100_000L, "second_charge").getResponse().getStatus());

        MvcResult first = withdraw(1, AccountType.USER, 100_000L, "first_withdraw");
        assertEquals(200, first.getResponse().getStatus());
        assertEquals(17_000L, data(first).path("rewardForfeitedCent").asLong());
        assertEquals(17_000L, data(first).path("feeCent").asLong());
        assertEquals(83_000L, data(first).path("netCent").asLong());
        assertEquals(117_000L, balance(1));

        MvcResult second = withdraw(1, AccountType.USER, 100_000L, "second_withdraw");
        assertEquals(200, second.getResponse().getStatus());
        assertEquals(0L, data(second).path("rewardForfeitedCent").asLong());
        assertEquals(10_000L, data(second).path("feeCent").asLong());
        assertEquals(17_000L, balance(1));
        assertEquals(5, transactionCount());
    }

    @Test
    void frozenBalanceAndLedgerFailureDoNotLoseMoney() throws Exception {
        jdbc.update("UPDATE virtual_wallet SET balance_cent=5000, frozen_balance_cent=1000 WHERE id=1");

        assertEquals(409,
                withdraw(1, AccountType.USER, 4001L, "too_much")
                        .getResponse().getStatus());
        assertEquals(5000L, balance(1));

        assertEquals(500,
                withdraw(1, AccountType.USER, 2000L, "FAIL_LEDGER")
                        .getResponse().getStatus());
        assertEquals(5000L, balance(1));
        assertEquals(0, transactionCount());

        MvcResult success = withdraw(1, AccountType.USER, 4000L, "available");
        assertEquals(200, success.getResponse().getStatus());
        assertEquals(400L, data(success).path("feeCent").asLong());
        assertEquals(3600L, data(success).path("netCent").asLong());
        assertEquals(1000L, balance(1));
        assertEquals(1000L, frozen(1));
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L, 10_000_000_000L})
    void rejectsInvalidAmounts(long amount) throws Exception {
        assertEquals(400,
                withdraw(1, AccountType.USER, amount, "invalid")
                        .getResponse().getStatus());
        assertEquals(0L, balance(1));
        assertEquals(0, transactionCount());
    }

    @Test
    void validatesRolesAndSupportsMerchantWallets() throws Exception {
        mvc.perform(post("/api/v1/wallets/me/withdrawals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amountCent\":100,\"requestId\":\"anon\"}"))
                .andExpect(status().isUnauthorized());

        assertEquals(403,
                withdraw(1, AccountType.RIDER, 100L, "rider")
                        .getResponse().getStatus());

        jdbc.update("UPDATE users SET status=0 WHERE id=1");
        assertEquals(403,
                withdraw(1, AccountType.USER, 100L, "disabled")
                        .getResponse().getStatus());

        jdbc.update("UPDATE virtual_wallet SET balance_cent=1000 WHERE id=3");
        MvcResult merchant = withdraw(1, AccountType.MERCHANT, 500L, "merchant");
        assertEquals(200, merchant.getResponse().getStatus());
        assertEquals(450L, data(merchant).path("netCent").asLong());
        assertEquals(500L, balance(3));
    }

    @Test
    void requestKeyCannotBeReusedForAnotherTransactionType() throws Exception {
        assertEquals(200, recharge(100L, "used_key").getResponse().getStatus());

        assertEquals(409,
                withdraw(1, AccountType.USER, 100L, "used_key")
                        .getResponse().getStatus());

        assertEquals(100L, balance(1));
        assertEquals(1, transactionCount());
    }

    @Test
    void simultaneousRetriesAreAppliedOnce() throws Exception {
        jdbc.update("UPDATE virtual_wallet SET balance_cent=10000 WHERE id=1");

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<MvcResult> action = () -> {
                ready.countDown();
                if (!start.await(20, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("start timeout");
                }
                return withdraw(1, AccountType.USER, 1000L, "parallel");
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

        assertEquals(9000L, balance(1));
        assertEquals(1, transactionCount());
    }

    private MvcResult recharge(long amount, String requestId) throws Exception {
        return mvc.perform(post("/api/v1/wallets/me/recharges")
                        .header("Authorization", bearer(1, AccountType.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "amountCent", amount,
                                "requestId", requestId))))
                .andReturn();
    }

    private MvcResult withdraw(long id, AccountType type, long amount, String requestId)
            throws Exception {
        return mvc.perform(post("/api/v1/wallets/me/withdrawals")
                        .header("Authorization", bearer(id, type))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "amountCent", amount,
                                "requestId", requestId))))
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

    private int transactionCount() {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM wallet_transaction",
                Integer.class);
    }
}
