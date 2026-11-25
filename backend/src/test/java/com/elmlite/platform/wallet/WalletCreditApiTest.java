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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:wallet_credit_api;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;LOCK_TIMEOUT=10000")
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Sql(scripts = {
        "/db/h2/admin-schema.sql",
        "/db/h2/wallet-credit-reset.sql",
        "/db/h2/wallet-recharge-schema.sql",
        "/db/h2/wallet-credit-schema.sql"
})
class WalletCreditApiTest {

    @Autowired private MockMvc mvc;
    @Autowired private JwtTokenService tokens;
    @Autowired private ObjectMapper json;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void unconfiguredWalletHasNoCredit() throws Exception {
        mvc.perform(get("/api/v1/wallets/me/credit")
                        .header("Authorization", bearer(1, AccountType.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.configured").value(false))
                .andExpect(jsonPath("$.data.creditLimitCent").value(0))
                .andExpect(jsonPath("$.data.totalDebtCent").value(0))
                .andExpect(jsonPath("$.data.availableCreditCent").value(0));
    }

    @Test
    void administratorConfiguresCreditForUserWallet() throws Exception {
        MvcResult result = configure(1L, 100_000L, 7, 5);
        assertEquals(200, result.getResponse().getStatus());
        assertEquals(100_000L, data(result).path("creditLimitCent").asLong());

        mvc.perform(get("/api/v1/wallets/me/credit")
                        .header("Authorization", bearer(1, AccountType.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.configured").value(true))
                .andExpect(jsonPath("$.data.creditLimitCent").value(100_000))
                .andExpect(jsonPath("$.data.repaymentDays").value(7))
                .andExpect(jsonPath("$.data.dailyRateBasisPoints").value(5))
                .andExpect(jsonPath("$.data.availableCreditCent").value(100_000))
                .andExpect(jsonPath("$.data.totalDebtCent").value(0));

        MvcResult updated = configure(1L, 200_000L, 14, 8);
        assertEquals(200, updated.getResponse().getStatus());
        assertEquals(200_000L, data(updated).path("creditLimitCent").asLong());
        assertEquals(1, creditCount());
    }

    @Test
    void calculatesOverdueDebtAndProtectsExistingTerms() throws Exception {
        assertEquals(200, configure(1L, 100_000L, 7, 5).getResponse().getStatus());

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        LocalDate due = today.minusDays(3);

        jdbc.update(
                "UPDATE wallet_credit_account SET principal_cent=10000, " +
                "due_date=?, interest_calculated_through=? WHERE wallet_id=1",
                due, due);

        mvc.perform(get("/api/v1/wallets/me/credit")
                        .header("Authorization", bearer(1, AccountType.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.principalCent").value(10_000))
                .andExpect(jsonPath("$.data.interestCent").value(15))
                .andExpect(jsonPath("$.data.totalDebtCent").value(10_015))
                .andExpect(jsonPath("$.data.availableCreditCent").value(89_985));

        assertEquals(409, configure(1L, 10_014L, 7, 5).getResponse().getStatus());
        assertEquals(409, configure(1L, 200_000L, 8, 5).getResponse().getStatus());
        assertEquals(409, configure(1L, 200_000L, 7, 6).getResponse().getStatus());

        MvcResult increased = configure(1L, 200_000L, 7, 5);
        assertEquals(200, increased.getResponse().getStatus());
        assertEquals(10_015L, data(increased).path("totalDebtCent").asLong());
    }

    @Test
    void rejectsDebtWithMissingDueDate() throws Exception {
        assertEquals(200, configure(1L, 100_000L, 7, 5).getResponse().getStatus());

        jdbc.update("UPDATE wallet_credit_account SET principal_cent=100 WHERE wallet_id=1");

        mvc.perform(get("/api/v1/wallets/me/credit")
                        .header("Authorization", bearer(1, AccountType.USER)))
                .andExpect(status().isConflict());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"creditLimitCent\":-1,\"repaymentDays\":7,\"dailyRateBasisPoints\":5}",
            "{\"creditLimitCent\":10000000000,\"repaymentDays\":7,\"dailyRateBasisPoints\":5}",
            "{\"creditLimitCent\":1000,\"repaymentDays\":0,\"dailyRateBasisPoints\":5}",
            "{\"creditLimitCent\":1000,\"repaymentDays\":3651,\"dailyRateBasisPoints\":5}",
            "{\"creditLimitCent\":1000,\"repaymentDays\":7,\"dailyRateBasisPoints\":10001}",
            "{\"creditLimitCent\":1000,\"repaymentDays\":7}"
    })
    void rejectsInvalidConfiguration(String body) throws Exception {
        mvc.perform(put("/api/v1/admin/wallets/1/credit")
                        .header("Authorization", bearer(1, AccountType.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void checksRolesAndAccountStatus() throws Exception {
        mvc.perform(get("/api/v1/wallets/me/credit"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/v1/wallets/me/credit")
                        .header("Authorization", bearer(1, AccountType.MERCHANT)))
                .andExpect(status().isForbidden());

        mvc.perform(put("/api/v1/admin/wallets/1/credit")
                        .header("Authorization", bearer(1, AccountType.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(1000L, 7, 5)))
                .andExpect(status().isForbidden());

        mvc.perform(put("/api/v1/admin/wallets/1/credit")
                        .header("Authorization", bearer(2, AccountType.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(1000L, 7, 5)))
                .andExpect(status().isForbidden());

        jdbc.update("UPDATE admin_account SET status=0 WHERE id=1");
        assertEquals(403, configure(1L, 1000L, 7, 5).getResponse().getStatus());
    }

    @Test
    void rejectsMissingMerchantAndDisabledUserWallets() throws Exception {
        assertEquals(404, configure(999L, 1000L, 7, 5).getResponse().getStatus());
        assertEquals(403, configure(3L, 1000L, 7, 5).getResponse().getStatus());

        jdbc.update("UPDATE users SET status=0 WHERE id=1");
        assertEquals(403, configure(1L, 1000L, 7, 5).getResponse().getStatus());
    }

    private MvcResult configure(long walletId, long limit, int days, int rate)
            throws Exception {
        return mvc.perform(put("/api/v1/admin/wallets/{id}/credit", walletId)
                        .header("Authorization", bearer(1, AccountType.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(limit, days, rate)))
                .andReturn();
    }

    private String body(long limit, int days, int rate) throws Exception {
        return json.writeValueAsString(Map.of(
                "creditLimitCent", limit,
                "repaymentDays", days,
                "dailyRateBasisPoints", rate));
    }

    private String bearer(long id, AccountType type) {
        return "Bearer " + tokens.issue(id, type);
    }

    private com.fasterxml.jackson.databind.JsonNode data(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString()).path("data");
    }

    private int creditCount() {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM wallet_credit_account", Integer.class);
    }
}
