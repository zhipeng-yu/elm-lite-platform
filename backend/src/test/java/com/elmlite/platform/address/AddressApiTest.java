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
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
}
