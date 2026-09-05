package com.elmlite.platform.address;

import com.elmlite.platform.mapper.DeliveryAddressMapper;
import com.elmlite.platform.service.JwtTokenService;
import com.elmlite.platform.service.JwtTokenService.AccountType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:address_api;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Sql("/db/h2/address-api-schema.sql")
class AddressApiTest {
    private static final String URL = "/api/v1/addresses";

    @Autowired private MockMvc mvc;
    @Autowired private JwtTokenService tokens;
    @Autowired private DeliveryAddressMapper addresses;
    @Autowired private ObjectMapper json;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void listsOnlyCurrentUsersAddressesInDefaultThenNewestOrder() throws Exception {
        mvc.perform(get(URL).header("Authorization", user(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[*].id", contains(1, 3, 2)))
                .andExpect(jsonPath("$.data[0].userId").doesNotExist());
    }

    @Test
    void readsContractFieldsWithoutOwnerOrTimestamps() throws Exception {
        mvc.perform(get(URL + "/2").header("Authorization", user(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(2))
                .andExpect(jsonPath("$.data.receiverName").value("测试收货人"))
                .andExpect(jsonPath("$.data.receiverPhone").value("19900000001"))
                .andExpect(jsonPath("$.data.addressDetail").value("二号楼"))
                .andExpect(jsonPath("$.data.addressLabel").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.data.isDefault").value(0))
                .andExpect(jsonPath("$.data.userId").doesNotExist())
                .andExpect(jsonPath("$.data.createdAt").doesNotExist())
                .andExpect(jsonPath("$.data.updatedAt").doesNotExist());
    }

    @Test
    void returnsEmptyArray() throws Exception {
        addresses.deleteById(4L);
        mvc.perform(get(URL).header("Authorization", user(2)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"/4,403", "/999,404", "/invalid,400"})
    void rejectsForeignMissingOrMalformedIds(String path, int expected) throws Exception {
        mvc.perform(get(URL + path).header("Authorization", user(1)))
                .andExpect(status().is(expected)).andExpect(jsonPath("$.code").value(expected));
    }

    @ParameterizedTest
    @CsvSource({"3,403", "999,404"})
    void validatesCurrentUserForEveryRead(long id, int expected) throws Exception {
        for (String path : new String[]{"", "/1"}) {
            mvc.perform(get(URL + path).header("Authorization", user(id)))
                    .andExpect(status().is(expected)).andExpect(jsonPath("$.code").value(expected));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/1"})
    void rejectsAnonymousAndMerchantReads(String path) throws Exception {
        mvc.perform(get(URL + path)).andExpect(status().isUnauthorized());
        mvc.perform(get(URL + path).header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get(URL + path).header("Authorization", "Bearer " + tokens.issue(1, AccountType.MERCHANT)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(403));
    }

    private String user(long id) {
        return "Bearer " + tokens.issue(id, AccountType.USER);
    }

    @Test
    void patchPreservesOmittedFieldsAndTrimsSubmittedText() throws Exception {
        mvc.perform(patch(URL + "/1").header("Authorization", user(1)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receiverName\":\"  新收货人  \"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.receiverName").value("新收货人"))
                .andExpect(jsonPath("$.data.receiverPhone").value("19900000001"))
                .andExpect(jsonPath("$.data.addressDetail").value("一号楼"))
                .andExpect(jsonPath("$.data.addressLabel").value("学校"))
                .andExpect(jsonPath("$.data.isDefault").value(1));
        assertEquals("新收货人", addresses.selectById(1L).getReceiverName());
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "\"\"", "\"  \""})
    void patchCanClearAddressLabel(String value) throws Exception {
        mvc.perform(patch(URL + "/1").header("Authorization", user(1)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressLabel\":" + value + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.addressLabel").value(org.hamcrest.Matchers.nullValue()));
        assertNull(addresses.selectById(1L).getAddressLabel());
    }

    @Test
    void patchChangesAllSubmittedFields() throws Exception {
        mvc.perform(patch(URL + "/2").header("Authorization", user(1)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receiverName":"新收货人","receiverPhone":" 19900000003 ",
                                 "addressDetail":" 新地址 ","addressLabel":" 家 ","isDefault":0}
                                """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.receiverPhone").value("19900000003"))
                .andExpect(jsonPath("$.data.addressDetail").value("新地址"))
                .andExpect(jsonPath("$.data.addressLabel").value("家"));
        assertEquals("新地址", addresses.selectById(2L).getAddressDetail());
    }

    @Test
    void patchSwitchesAndCancelsDefaultWithoutAffectingOtherUsers() throws Exception {
        for (int value : new int[]{1, 0}) {
            mvc.perform(patch(URL + "/2").header("Authorization", user(1)).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"isDefault\":" + value + "}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.isDefault").value(value));
            assertEquals(0, addresses.selectById(1L).getIsDefault());
            assertEquals(1, addresses.selectById(4L).getIsDefault());
        }
        assertEquals(0, addresses.selectById(2L).getIsDefault());
    }

    @ParameterizedTest
    @ValueSource(strings = {"receiverName", "receiverPhone", "addressDetail", "isDefault"})
    void rejectsExplicitNullInRequiredPatchFields(String field) throws Exception {
        mvc.perform(patch(URL + "/1").header("Authorization", user(1)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"" + field + "\":null}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.data.fieldErrors." + field).isString());
        assertEquals(1, addresses.selectById(1L).getIsDefault());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"userId\":2}", "{\"addressLabel\":5}", "{\"isDefault\":1.5}",
            "{\"isDefault\":2}", "{\"receiverName\":\" \"}", "{\"receiverPhone\":\"invalid\"}"})
    void rejectsInvalidPatchWithoutChangingData(String body) throws Exception {
        mvc.perform(patch(URL + "/1").header("Authorization", user(1)).contentType(MediaType.APPLICATION_JSON)
                        .content(body)).andExpect(status().isBadRequest());
        assertEquals("测试收货人", addresses.selectById(1L).getReceiverName());
        assertEquals(1, addresses.selectById(1L).getIsDefault());
    }

    @ParameterizedTest
    @CsvSource({"4,403", "999,404", "invalid,400"})
    void updateAndDeleteRejectForeignMissingAndMalformedIds(String id, int expected) throws Exception {
        mvc.perform(patch(URL + "/" + id).header("Authorization", user(1)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"isDefault\":1}")).andExpect(status().is(expected));
        mvc.perform(delete(URL + "/" + id).header("Authorization", user(1))).andExpect(status().is(expected));
        assertEquals(4L, addresses.selectCount(null));
        assertEquals(1, addresses.selectById(4L).getIsDefault());
    }

    @Test
    void updateAndDeleteRequireActiveUserIdentity() throws Exception {
        String[] auth = {null, "Bearer invalid-token", "Bearer " + tokens.issue(1, AccountType.MERCHANT), user(3), user(999)};
        int[] expected = {401, 401, 403, 403, 404};
        for (int i = 0; i < auth.length; i++) {
            var update = patch(URL + "/1").contentType(MediaType.APPLICATION_JSON).content("{\"isDefault\":0}");
            var remove = delete(URL + "/1");
            if (auth[i] != null) { update.header("Authorization", auth[i]); remove.header("Authorization", auth[i]); }
            mvc.perform(update).andExpect(status().is(expected[i]));
            mvc.perform(remove).andExpect(status().is(expected[i]));
        }
        assertEquals(4L, addresses.selectCount(null));
    }

    @Test
    void deletesDefaultWithoutChoosingReplacementAndRejectsRepeat() throws Exception {
        mvc.perform(delete(URL + "/1").header("Authorization", user(1)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));
        assertNull(addresses.selectById(1L));
        assertEquals(0, addresses.selectById(2L).getIsDefault());
        assertEquals(0, addresses.selectById(3L).getIsDefault());
        assertEquals(1, addresses.selectById(4L).getIsDefault());
        mvc.perform(delete(URL + "/1").header("Authorization", user(1))).andExpect(status().isNotFound());
    }

    @Test
    void deletingAddressClearsOrderReferenceButPreservesSnapshot() throws Exception {
        jdbc.execute("""
                CREATE TABLE orders (id BIGINT PRIMARY KEY, address_id BIGINT,
                    receiver_name VARCHAR(50), receiver_phone VARCHAR(20), delivery_address VARCHAR(255),
                    FOREIGN KEY (address_id) REFERENCES delivery_address(id) ON DELETE SET NULL)
                """);
        jdbc.update("INSERT INTO orders VALUES (1, 1, '下单时收货人', '19900000001', '下单时地址')");
        mvc.perform(delete(URL + "/1").header("Authorization", user(1))).andExpect(status().isOk());
        assertNull(jdbc.queryForObject("SELECT address_id FROM orders WHERE id=1", Long.class));
        assertEquals("下单时收货人", jdbc.queryForObject("SELECT receiver_name FROM orders WHERE id=1", String.class));
        assertEquals("19900000001", jdbc.queryForObject("SELECT receiver_phone FROM orders WHERE id=1", String.class));
        assertEquals("下单时地址", jdbc.queryForObject("SELECT delivery_address FROM orders WHERE id=1", String.class));
    }

    @Test
    void writeFailureRollsBackDefaultSwitch() throws Exception {
        // 故意让最终写入失败，验证已经执行的取消旧默认也会回滚。
        jdbc.execute("ALTER TABLE delivery_address ADD CONSTRAINT test_write_failure CHECK (receiver_name <> '拒绝写入')");
        var body = validBody(); body.put("receiverName", "拒绝写入"); body.put("isDefault", 1);
        mvc.perform(post(URL).header("Authorization", user(1)).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body))).andExpect(status().isInternalServerError());
        assertEquals(1, addresses.selectById(1L).getIsDefault());
        mvc.perform(patch(URL + "/2").header("Authorization", user(1)).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body))).andExpect(status().isInternalServerError());
        assertEquals(1, addresses.selectById(1L).getIsDefault());
        assertEquals(0, addresses.selectById(2L).getIsDefault());
        assertEquals(4L, addresses.selectCount(null));
    }

    @Test
    void concurrentDefaultChangesLeaveOnlyOneDefault() throws Exception {
        jdbc.update("UPDATE delivery_address SET is_default=0 WHERE user_id=1");
        var ready = new java.util.concurrent.CyclicBarrier(2);
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        String authorization = user(1);
        try {
            var first = executor.submit(() -> {
                ready.await(5, java.util.concurrent.TimeUnit.SECONDS);
                return mvc.perform(patch(URL + "/2").header("Authorization", authorization)
                                .contentType(MediaType.APPLICATION_JSON).content("{\"isDefault\":1}"))
                        .andReturn().getResponse().getStatus();
            });
            var second = executor.submit(() -> {
                ready.await(5, java.util.concurrent.TimeUnit.SECONDS);
                return mvc.perform(patch(URL + "/3").header("Authorization", authorization)
                                .contentType(MediaType.APPLICATION_JSON).content("{\"isDefault\":1}"))
                        .andReturn().getResponse().getStatus();
            });
            assertEquals(200, first.get(10, java.util.concurrent.TimeUnit.SECONDS));
            assertEquals(200, second.get(10, java.util.concurrent.TimeUnit.SECONDS));
            assertEquals(1, jdbc.queryForObject(
                    "SELECT COUNT(*) FROM delivery_address WHERE user_id=1 AND is_default=1", Integer.class));
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);
        }
    }

    @Test
    void createsTrimmedAddressForCurrentUserWithoutAutoDefault() throws Exception {
        addresses.deleteById(4L);
        String response = mvc.perform(post(URL).header("Authorization", user(2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receiverName":"  收货人  ","receiverPhone":" 19900000002 ",
                                 "addressDetail":"  学校 一号楼  ","addressLabel":"  "}
                                """))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.receiverName").value("收货人"))
                .andExpect(jsonPath("$.data.addressDetail").value("学校 一号楼"))
                .andExpect(jsonPath("$.data.isDefault").value(0))
                .andExpect(jsonPath("$.data.addressLabel").value(org.hamcrest.Matchers.nullValue()))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(response).path("data").path("id").asLong();
        assertEquals(2L, addresses.selectById(id).getUserId());
    }

    @Test
    void creatingDefaultReplacesOnlyCurrentUsersDefault() throws Exception {
        var body = validBody();
        body.put("isDefault", 1);
        mvc.perform(post(URL).header("Authorization", user(1)).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.isDefault").value(1));
        assertEquals(0, addresses.selectById(1L).getIsDefault());
        assertEquals(1, addresses.selectById(4L).getIsDefault());
        assertEquals(5L, addresses.selectCount(null));
    }

    @ParameterizedTest
    @CsvSource({"receiverName,51", "addressDetail,256", "addressLabel,21"})
    void rejectsOversizedCreateFields(String field, int length) throws Exception {
        var body = validBody(); body.put(field, "字".repeat(length));
        expectInvalidCreate(body, field);
    }

    @ParameterizedTest
    @ValueSource(strings = {"receiverName", "receiverPhone", "addressDetail"})
    void rejectsMissingNullOrBlankRequiredCreateFields(String field) throws Exception {
        var body = validBody(); body.remove(field); expectInvalidCreate(body, field);
        body.put(field, null); expectInvalidCreate(body, field);
        body.put(field, "  "); expectInvalidCreate(body, field);
    }

    @ParameterizedTest
    @ValueSource(strings = {"29900000001", "1990000000", "199000000001", "+8619900000001", "1990000000a"})
    void rejectsInvalidPhone(String phone) throws Exception {
        var body = validBody(); body.put("receiverPhone", phone); expectInvalidCreate(body, "receiverPhone");
    }

    @Test
    void rejectsNullAndOutOfRangeDefault() throws Exception {
        for (Integer value : new Integer[]{null, -1, 2}) {
            var body = validBody(); body.put("isDefault", value); expectInvalidCreate(body, "isDefault");
        }
    }

    @Test
    void acceptsMaximumFieldLengths() throws Exception {
        var body = validBody();
        body.put("receiverName", "字".repeat(50)); body.put("addressDetail", "字".repeat(255));
        body.put("addressLabel", "字".repeat(20));
        mvc.perform(post(URL).header("Authorization", user(1)).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body))).andExpect(status().isCreated());
    }

    @ParameterizedTest
    @ValueSource(strings = {"userId", "id", "createdAt", "updatedAt", "unexpected"})
    void rejectsExtraCreateFields(String field) throws Exception {
        var body = validBody(); body.put(field, 2);
        expectInvalidCreate(body, null);
    }

    @Test
    void rejectsWrongJsonTypesAndMalformedBodies() throws Exception {
        for (Object value : new Object[]{true, "1", 1.5}) {
            var body = validBody(); body.put("isDefault", value); expectInvalidCreate(body, null);
        }
        var body = validBody(); body.put("receiverPhone", 19900000001L); expectInvalidCreate(body, null);
        for (String content : new String[]{"[]", "null", "{", ""}) {
            mvc.perform(post(URL).header("Authorization", user(1)).contentType(MediaType.APPLICATION_JSON)
                    .content(content)).andExpect(status().isBadRequest());
        }
        assertEquals(4L, addresses.selectCount(null));
    }

    @Test
    void creationRequiresActiveUserIdentity() throws Exception {
        String body = json.writeValueAsString(validBody());
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mvc.perform(post(URL).header("Authorization", "Bearer " + tokens.issue(1, AccountType.MERCHANT))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mvc.perform(post(URL).header("Authorization", user(3)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mvc.perform(post(URL).header("Authorization", user(999)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
        assertEquals(4L, addresses.selectCount(null));
    }

    private java.util.Map<String, Object> validBody() {
        var body = new java.util.LinkedHashMap<String, Object>();
        body.put("receiverName", "收货人"); body.put("receiverPhone", "19900000001");
        body.put("addressDetail", "学校一号楼");
        return body;
    }

    private void expectInvalidCreate(java.util.Map<String, Object> body, String field) throws Exception {
        var result = mvc.perform(post(URL).header("Authorization", user(1)).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400));
        if (field != null) result.andExpect(jsonPath("$.data.fieldErrors." + field).isString());
        assertEquals(4L, addresses.selectCount(null));
        assertEquals(1, addresses.selectById(1L).getIsDefault());
    }
}
