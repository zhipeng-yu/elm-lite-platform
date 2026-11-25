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
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:wallet_withdrawal_rule_api;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Sql(scripts = {
        "/db/h2/admin-schema.sql",
        "/db/h2/wallet-credit-reset.sql",
        "/db/h2/wallet-recharge-schema.sql",
        "/db/h2/wallet-credit-schema.sql",
        "/db/h2/wallet-withdrawal-rule-schema.sql"
})
class WalletWithdrawalRuleApiTest {

    @Autowired private MockMvc mvc;
    @Autowired private JwtTokenService tokens;
    @Autowired private ObjectMapper json;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void administratorChangesRateAndRetryKeepsOriginalFee() throws Exception {
        mvc.perform(get("/api/v1/admin/wallet/withdrawal-rule")
                        .header("Authorization", bearer(1, AccountType.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.feeBasisPoints").value(1000));

        update(500).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.feeBasisPoints").value(500));

        jdbc.update("UPDATE virtual_wallet SET balance_cent=10000 WHERE id=1");

        MvcResult first = withdraw(10_000L, "rate_change");
        assertEquals(200, first.getResponse().getStatus());
        assertEquals(500L, data(first).path("feeCent").asLong());
        assertEquals(9500L, data(first).path("netCent").asLong());

        update(1500).andExpect(status().isOk());

        MvcResult retry = withdraw(10_000L, "rate_change");
        assertEquals(200, retry.getResponse().getStatus());
        assertEquals(data(first).path("transactionId").asLong(),
                data(retry).path("transactionId").asLong());
        assertEquals(500L, data(retry).path("feeCent").asLong());
        assertEquals(0L, balance());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"feeBasisPoints\":-1}",
            "{\"feeBasisPoints\":10000}",
            "{\"feeBasisPoints\":1.5}",
            "{}"
    })
    void rejectsInvalidRates(String body) throws Exception {
        mvc.perform(put("/api/v1/admin/wallet/withdrawal-rule")
                        .header("Authorization", bearer(1, AccountType.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void checksRolesAndDisabledAdministrators() throws Exception {
        mvc.perform(get("/api/v1/admin/wallet/withdrawal-rule"))
                .andExpect(status().isUnauthorized());

        for (AccountType type : new AccountType[]{
                AccountType.USER, AccountType.MERCHANT, AccountType.RIDER}) {
            mvc.perform(get("/api/v1/admin/wallet/withdrawal-rule")
                            .header("Authorization", bearer(1, type)))
                    .andExpect(status().isForbidden());
        }

        jdbc.update("UPDATE admin_account SET status=0 WHERE id=1");

        mvc.perform(get("/api/v1/admin/wallet/withdrawal-rule")
                        .header("Authorization", bearer(1, AccountType.ADMIN)))
                .andExpect(status().isForbidden());
    }

    @Test
    void supportsZeroAndMaximumRates() throws Exception {
        update(0).andExpect(status().isOk());
        jdbc.update("UPDATE virtual_wallet SET balance_cent=10000 WHERE id=1");

        MvcResult zero = withdraw(10_000L, "zero_rate");
        assertEquals(200, zero.getResponse().getStatus());
        assertEquals(0L, data(zero).path("feeCent").asLong());
        assertEquals(10_000L, data(zero).path("netCent").asLong());

        update(9999).andExpect(status().isOk());
        jdbc.update("UPDATE virtual_wallet SET balance_cent=10000 WHERE id=1");

        MvcResult maximum = withdraw(10_000L, "max_rate");
        assertEquals(200, maximum.getResponse().getStatus());
        assertEquals(9999L, data(maximum).path("feeCent").asLong());
        assertEquals(1L, data(maximum).path("netCent").asLong());
    }

    @Test
    void missingRuleDoesNotChangeBalance() throws Exception {
        jdbc.update("UPDATE virtual_wallet SET balance_cent=5000 WHERE id=1");
        jdbc.update("DELETE FROM wallet_withdrawal_rule WHERE id=1");

        mvc.perform(get("/api/v1/admin/wallet/withdrawal-rule")
                        .header("Authorization", bearer(1, AccountType.ADMIN)))
                .andExpect(status().isConflict());

        update(1000).andExpect(status().isConflict());

        assertEquals(409, withdraw(1000L, "no_rule").getResponse().getStatus());
        assertEquals(5000L, balance());
        assertEquals(0, transactionCount());
    }

    @Test
    void rewardIsStillForfeitedWhenConfiguredRateIsLower() throws Exception {
        update(500).andExpect(status().isOk());

        MvcResult recharge = mvc.perform(post("/api/v1/wallets/me/recharges")
                        .header("Authorization", bearer(1, AccountType.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "amountCent", 100_000L,
                                "requestId", "reward_charge"))))
                .andReturn();

        assertEquals(200, recharge.getResponse().getStatus());

        MvcResult withdrawal = withdraw(110_000L, "reward_withdrawal");
        assertEquals(200, withdrawal.getResponse().getStatus());
        assertEquals(10_000L, data(withdrawal).path("feeCent").asLong());
        assertEquals(10_000L, data(withdrawal).path("rewardForfeitedCent").asLong());
        assertEquals(100_000L, data(withdrawal).path("netCent").asLong());
    }

    private ResultActions update(int basisPoints) throws Exception {
        return mvc.perform(put("/api/v1/admin/wallet/withdrawal-rule")
                        .header("Authorization", bearer(1, AccountType.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "feeBasisPoints", basisPoints))));
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
                "SELECT balance_cent FROM virtual_wallet WHERE id=1",
                Long.class);
    }

    private int transactionCount() {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM wallet_transaction",
                Integer.class);
    }
}
