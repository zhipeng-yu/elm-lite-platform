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

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:wallet_credit_operation_api;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;LOCK_TIMEOUT=10000")
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Sql(scripts = {
        "/db/h2/admin-schema.sql",
        "/db/h2/wallet-credit-reset.sql",
        "/db/h2/wallet-recharge-schema.sql",
        "/db/h2/wallet-credit-schema.sql",
        "/db/h2/wallet-withdrawal-rule-schema.sql"
})
class WalletCreditOperationApiTest {

    @Autowired private MockMvc mvc;
    @Autowired private JwtTokenService tokens;
    @Autowired private ObjectMapper json;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void borrowsRepaysAndBlocksWithdrawalWhileInDebt() throws Exception {
        configure(10_000L);

        MvcResult first = operation("borrows", 4000L, "borrow_one");
        assertEquals(200, first.getResponse().getStatus());
        assertEquals(4000L, balance());
        assertEquals(4000L, principal());

        MvcResult retry = operation("borrows", 4000L, "borrow_one");
        assertEquals(200, retry.getResponse().getStatus());
        assertEquals(data(first).path("transactionId").asLong(),
                data(retry).path("transactionId").asLong());

        assertEquals(409,
                operation("borrows", 4001L, "borrow_one").getResponse().getStatus());
        assertEquals(409,
                withdraw(1000L, "blocked_withdrawal").getResponse().getStatus());

        MvcResult partial = operation("repayments", 2500L, "repay_one");
        assertEquals(200, partial.getResponse().getStatus());
        assertEquals(2500L, data(partial).path("principalPaidCent").asLong());
        assertEquals(0L, data(partial).path("interestPaidCent").asLong());
        assertEquals(1500L, principal());
        assertEquals(1500L, balance());

        MvcResult finalRepayment = operation("repayments", 1500L, "repay_two");
        assertEquals(200, finalRepayment.getResponse().getStatus());
        assertEquals(0L, principal());
        assertEquals(0L, balance());

        mvc.perform(get("/api/v1/wallets/me/credit")
                        .header("Authorization", bearer(1, AccountType.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalDebtCent").value(0))
                .andExpect(jsonPath("$.data.dueDate").doesNotExist());

        assertEquals(200, recharge(10_000L, "after_repayment").getResponse().getStatus());
        assertEquals(200, withdraw(10_000L, "allowed_withdrawal").getResponse().getStatus());
        assertEquals(5, transactionCount());
    }

    @Test
    void repaymentCannotUseFrozenFundsOrUnspentReward() throws Exception {
        configure(20_000L);
        assertEquals(200, recharge(100_000L, "reward_charge").getResponse().getStatus());
        assertEquals(200, operation("borrows", 10_000L, "reward_borrow").getResponse().getStatus());

        jdbc.update("UPDATE virtual_wallet SET frozen_balance_cent=109500 WHERE id=1");

        assertEquals(409,
                operation("repayments", 1000L, "too_little_cash")
                        .getResponse().getStatus());

        assertEquals(120_000L, balance());
        assertEquals(10_000L, principal());

        jdbc.update("UPDATE virtual_wallet SET frozen_balance_cent=108000 WHERE id=1");

        assertEquals(200,
                operation("repayments", 1000L, "enough_cash")
                        .getResponse().getStatus());

        assertEquals(119_000L, balance());
        assertEquals(9000L, principal());
    }

    @Test
    void overdueRepaymentPaysInterestBeforePrincipal() throws Exception {
        configure(100_000L);

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        LocalDate due = today.minusDays(3);

        jdbc.update(
                "UPDATE wallet_credit_account SET principal_cent=10000, " +
                "due_date=?, interest_calculated_through=? WHERE wallet_id=1",
                due, due);

        jdbc.update("UPDATE virtual_wallet SET balance_cent=20000 WHERE id=1");

        MvcResult result = operation("repayments", 1010L, "overdue_payment");
        assertEquals(200, result.getResponse().getStatus());
        assertEquals(15L, data(result).path("interestPaidCent").asLong());
        assertEquals(995L, data(result).path("principalPaidCent").asLong());
        assertEquals(9005L, principal());
        assertEquals(18_990L, balance());

        mvc.perform(get("/api/v1/wallets/me/credit")
                        .header("Authorization", bearer(1, AccountType.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.principalCent").value(9005))
                .andExpect(jsonPath("$.data.interestCent").value(0))
                .andExpect(jsonPath("$.data.totalDebtCent").value(9005));
    }

    @Test
    void checksLimitsRolesAndMissingConfiguration() throws Exception {
        assertEquals(409, operation("borrows", 1000L, "no_credit").getResponse().getStatus());

        mvc.perform(post("/api/v1/wallets/me/credit/borrows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amountCent\":100,\"requestId\":\"anon\"}"))
                .andExpect(status().isUnauthorized());

        assertEquals(403,
                operationAs(1, AccountType.MERCHANT, "borrows", 100L, "merchant")
                        .getResponse().getStatus());

        configure(1000L);
        assertEquals(409, operation("borrows", 1001L, "over_limit").getResponse().getStatus());
        assertEquals(0L, balance());
        assertEquals(0L, principal());
    }

    @Test
    void ledgerFailureRollsBackWalletAndDebt() throws Exception {
        configure(5000L);

        assertEquals(500,
                operation("borrows", 1000L, "FAIL_LEDGER").getResponse().getStatus());
        assertEquals(0L, balance());
        assertEquals(0L, principal());
        assertEquals(0, transactionCount());

        assertEquals(200,
                operation("borrows", 1000L, "successful_borrow").getResponse().getStatus());

        assertEquals(500,
                operation("repayments", 500L, "FAIL_LEDGER").getResponse().getStatus());

        assertEquals(1000L, balance());
        assertEquals(1000L, principal());
        assertEquals(1, transactionCount());
    }

    @Test
    void differentOperationCannotReuseRequestKey() throws Exception {
        configure(5000L);

        assertEquals(200, operation("borrows", 1000L, "shared_key").getResponse().getStatus());
        assertEquals(409, operation("repayments", 1000L, "shared_key").getResponse().getStatus());

        assertEquals(1000L, balance());
        assertEquals(1000L, principal());
        assertEquals(1, transactionCount());
    }

    @Test
    void concurrentBorrowRetriesAreAppliedOnce() throws Exception {
        configure(10_000L);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<MvcResult> action = () -> {
                ready.countDown();

                if (!start.await(20, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("start timeout");
                }

                return operation("borrows", 1000L, "parallel_borrow");
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

        assertEquals(1000L, balance());
        assertEquals(1000L, principal());
        assertEquals(1, transactionCount());
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L, 10_000_000_000L})
    void rejectsInvalidAmounts(long amount) throws Exception {
        assertEquals(400, operation("borrows", amount, "invalid").getResponse().getStatus());
        assertEquals(0L, balance());
        assertEquals(0, transactionCount());
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

    private MvcResult operation(String action, long amount, String requestId)
            throws Exception {
        return operationAs(1, AccountType.USER, action, amount, requestId);
    }

    private MvcResult operationAs(
            long id, AccountType type, String action, long amount, String requestId)
            throws Exception {
        return mvc.perform(post("/api/v1/wallets/me/credit/" + action)
                        .header("Authorization", bearer(id, type))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "amountCent", amount,
                                "requestId", requestId))))
                .andReturn();
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

    private MvcResult withdraw(long amount, String requestId) throws Exception {
        return mvc.perform(post("/api/v1/wallets/me/withdrawals")
                        .header("Authorization", bearer(1, AccountType.USER))
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

    private long balance() {
        return jdbc.queryForObject(
                "SELECT balance_cent FROM virtual_wallet WHERE id=1", Long.class);
    }

    private long principal() {
        return jdbc.queryForObject(
                "SELECT principal_cent FROM wallet_credit_account WHERE wallet_id=1",
                Long.class);
    }

    private int transactionCount() {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM wallet_transaction", Integer.class);
    }
}
