package com.elmlite.platform.common;

import com.elmlite.platform.exception.BusinessException;
import com.elmlite.platform.exception.GlobalExceptionHandler;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GlobalExceptionHandlerTest.TestController.class)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void successResponseUsesUnifiedEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/test/success"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("success"))
                .andExpect(jsonPath("$.data.value").value("ok"));
    }

    @Test
    void validationFailureReturnsFieldErrors() throws Exception {
        mockMvc.perform(post("/api/v1/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("参数校验失败"))
                .andExpect(jsonPath("$.data.fieldErrors.name").value("名称不能为空"));
    }

    @Test
    void businessExceptionUsesItsHttpStatusAndMessage() throws Exception {
        mockMvc.perform(get("/api/v1/test/business-error"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.msg").value("资源状态冲突"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void malformedJsonReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("请求参数格式错误"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void unexpectedExceptionReturnsGenericServerError() throws Exception {
        mockMvc.perform(get("/api/v1/test/unexpected-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("服务器内部异常"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("sensitive detail"))));
    }

    @RestController
    static class TestController {

        @GetMapping("/api/v1/test/success")
        ApiResponse<Map<String, String>> success() {
            return ApiResponse.success(Map.of("value", "ok"));
        }

        @PostMapping("/api/v1/test/validate")
        ApiResponse<Void> validate(@Valid @RequestBody SampleRequest request) {
            return ApiResponse.success();
        }

        @GetMapping("/api/v1/test/business-error")
        void businessError() {
            throw new BusinessException(HttpStatus.CONFLICT, "资源状态冲突");
        }

        @GetMapping("/api/v1/test/unexpected-error")
        void unexpectedError() {
            throw new IllegalStateException("sensitive detail");
        }
    }

    record SampleRequest(@NotBlank(message = "名称不能为空") String name) {
    }
}
