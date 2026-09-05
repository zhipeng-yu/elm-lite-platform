package com.elmlite.platform.common;

import com.elmlite.platform.config.SecurityConfig;
import com.elmlite.platform.service.JwtTokenService;
import com.elmlite.platform.service.JwtTokenService.AccountType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(Day4SecurityTest.Probe.class)
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Import({SecurityConfig.class, JwtTokenService.class, Day4SecurityTest.Probe.class})
class Day4SecurityTest {
    @Autowired private MockMvc mvc;
    @Autowired private JwtTokenService tokens;

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/products/1", "/api/v1/shops/1/products", "/api/v1/shops/1/categories"})
    void publicGetDoesNotPermitAnonymousWrites(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isOk());
        mvc.perform(post(path)).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/merchant/categories/1", "/api/v1/merchant/products/1",
            "/api/v1/merchant/shops/1/categories", "/api/v1/merchant/shops/1/products"})
    void merchantManagementRejectsUserTokens(String path) throws Exception {
        mvc.perform(patch(path)).andExpect(status().isUnauthorized());
        mvc.perform(patch(path).header("Authorization", "Bearer " + tokens.issue(1, AccountType.USER)))
                .andExpect(status().isForbidden());
        mvc.perform(patch(path).header("Authorization", "Bearer " + tokens.issue(1, AccountType.MERCHANT)))
                .andExpect(status().isOk());
    }

    // 隔离鉴权测试，不实现或依赖梁的商品业务接口。
    @RestController
    static class Probe {
        @RequestMapping({"/api/v1/products/1", "/api/v1/shops/1/products", "/api/v1/shops/1/categories",
                "/api/v1/merchant/categories/1", "/api/v1/merchant/products/1",
                "/api/v1/merchant/shops/1/categories", "/api/v1/merchant/shops/1/products"})
        ApiResponse<Void> probe() { return ApiResponse.success(null); }
    }
}
