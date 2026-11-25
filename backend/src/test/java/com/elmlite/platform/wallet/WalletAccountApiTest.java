package com.elmlite.platform.wallet;

import com.elmlite.platform.service.JwtTokenService;
import com.elmlite.platform.service.JwtTokenService.AccountType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:wallet_account_api;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;LOCK_TIMEOUT=10000")
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Sql(scripts = {"/db/h2/user-schema.sql", "/db/h2/user-data.sql", "/db/h2/merchant-schema.sql", "/db/h2/merchant-data.sql", "/db/h2/wallet-account-schema.sql"})
class WalletAccountApiTest {
    @Autowired private MockMvc mvc;
    @Autowired private JwtTokenService tokens;
    @Autowired private ObjectMapper json;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void userCanOpenAndReadZeroBalanceWallet() throws Exception {
        var result = mvc.perform(post("/api/v1/wallets/me").header("Authorization", bearer(1, AccountType.USER)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.balanceCent").value(0))
                .andExpect(jsonPath("$.data.frozenBalanceCent").value(0))
                .andExpect(jsonPath("$.data.availableBalanceCent").value(0))
                .andReturn();
        long id = json.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
        mvc.perform(get("/api/v1/wallets/me").header("Authorization", bearer(1, AccountType.USER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(id));
        assertEquals(1, count());
    }

    @Test
    void queryingMissingWalletDoesNotCreateOne() throws Exception {
        mvc.perform(get("/api/v1/wallets/me").header("Authorization", bearer(1, AccountType.USER)))
                .andExpect(status().isNotFound());
        assertEquals(0, count());
    }

    @Test
    void duplicateOpeningReturnsConflict() throws Exception {
        mvc.perform(post("/api/v1/wallets/me").header("Authorization", bearer(1, AccountType.USER)))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/wallets/me").header("Authorization", bearer(1, AccountType.USER)))
                .andExpect(status().isConflict());
        assertEquals(1, count());
    }

    @Test
    void databaseRejectsInvalidOwnerAndNegativeFrozenAmount() {
        String sql = "INSERT INTO virtual_wallet(owner_type, owner_id, frozen_balance_cent) VALUES (?, ?, ?)";
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(sql, "ADMIN", 1L, 0L));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(sql, "USER", 0L, 0L));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(sql, "USER", 1L, -1L));
        assertEquals(0, count());
    }

    @Test
    void userAndMerchantWithSameNumericIdGetSeparateWallets() throws Exception {
        var user = mvc.perform(post("/api/v1/wallets/me").header("Authorization", bearer(1, AccountType.USER)))
                .andExpect(status().isCreated()).andReturn();
        var merchant = mvc.perform(post("/api/v1/wallets/me").header("Authorization", bearer(1, AccountType.MERCHANT)))
                .andExpect(status().isCreated()).andReturn();
        long userId = json.readTree(user.getResponse().getContentAsString()).path("data").path("id").asLong();
        long merchantId = json.readTree(merchant.getResponse().getContentAsString()).path("data").path("id").asLong();
        assertNotEquals(userId, merchantId);
        mvc.perform(get("/api/v1/wallets/me").header("Authorization", bearer(1, AccountType.USER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(userId));
        mvc.perform(get("/api/v1/wallets/me").header("Authorization", bearer(1, AccountType.MERCHANT)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(merchantId));
    }

    @Test
    void anonymousAndOtherRolesCannotUseWallets() throws Exception {
        mvc.perform(get("/api/v1/wallets/me")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/wallets/me")).andExpect(status().isUnauthorized());
        for (AccountType type : List.of(AccountType.ADMIN, AccountType.RIDER)) {
            mvc.perform(get("/api/v1/wallets/me").header("Authorization", bearer(1, type)))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/api/v1/wallets/me").header("Authorization", bearer(1, type)))
                    .andExpect(status().isForbidden());
        }
        assertEquals(0, count());
    }

    @Test
    void disabledAndMissingAccountsCannotOpenOrReadWallets() throws Exception {
        mvc.perform(post("/api/v1/wallets/me").header("Authorization", bearer(1, AccountType.USER)))
                .andExpect(status().isCreated());
        jdbc.update("UPDATE users SET status = 0 WHERE id = 1");
        mvc.perform(get("/api/v1/wallets/me").header("Authorization", bearer(1, AccountType.USER)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/wallets/me").header("Authorization", bearer(1, AccountType.USER)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/wallets/me").header("Authorization", bearer(999, AccountType.USER)))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/wallets/me").header("Authorization", bearer(999, AccountType.MERCHANT)))
                .andExpect(status().isNotFound());
        jdbc.update("UPDATE merchant SET status = 0 WHERE id = 1");
        mvc.perform(post("/api/v1/wallets/me").header("Authorization", bearer(1, AccountType.MERCHANT)))
                .andExpect(status().isForbidden());
    }

    @Test
    void concurrentOpeningCreatesOnlyOneWallet() throws Exception {
        String authorization = bearer(1, AccountType.USER);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Integer> action = () -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("start timeout");
                return mvc.perform(post("/api/v1/wallets/me").header("Authorization", authorization))
                        .andReturn().getResponse().getStatus();
            };
            Future<Integer> first = executor.submit(action);
            Future<Integer> second = executor.submit(action);
            if (!ready.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("ready timeout");
            start.countDown();
            assertEquals(List.of(201, 409), java.util.stream.Stream.of(
                    first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS)).sorted().toList());
        }
        assertEquals(1, count());
    }

    private String bearer(long id, AccountType type) {
        return "Bearer " + tokens.issue(id, type);
    }

    private int count() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM virtual_wallet", Integer.class);
    }
}
