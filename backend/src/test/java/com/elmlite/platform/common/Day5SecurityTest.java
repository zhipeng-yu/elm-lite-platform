package com.elmlite.platform.common;

import com.elmlite.platform.config.SecurityConfig;
import com.elmlite.platform.service.JwtTokenService;
import com.elmlite.platform.service.JwtTokenService.AccountType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(Day5SecurityTest.Probe.class)
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Import({SecurityConfig.class, JwtTokenService.class, Day5SecurityTest.Probe.class})
class Day5SecurityTest {
    @Autowired private MockMvc mvc;
    @Autowired private JwtTokenService tokens;

    @ParameterizedTest
    @CsvSource({"GET,/api/v1/cart/items", "POST,/api/v1/cart/items",
            "PATCH,/api/v1/cart/items/1", "DELETE,/api/v1/cart/items/1",
            "GET,/api/v1/orders", "POST,/api/v1/orders", "GET,/api/v1/orders/1"})
    void onlyUserTokensCanReachCartAndOrderHandlers(String method, String path) throws Exception {
        HttpMethod verb = HttpMethod.valueOf(method);
        mvc.perform(request(verb, path)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
        mvc.perform(request(verb, path).header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(401));
        mvc.perform(request(verb, path).header("Authorization", "Bearer " + tokens.issue(1, AccountType.USER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));
        mvc.perform(request(verb, path).header("Authorization", "Bearer " + tokens.issue(1, AccountType.MERCHANT)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(403));
    }

    // 仅验证公共鉴权，不依赖购物车或订单业务实现。
    @TestComponent
    @RestController
    static class Probe {
        @RequestMapping({"/api/v1/cart/items", "/api/v1/cart/items/1", "/api/v1/orders", "/api/v1/orders/1"})
        ApiResponse<Void> probe() { return ApiResponse.success(null); }
    }
}
