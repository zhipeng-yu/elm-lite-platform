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
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
