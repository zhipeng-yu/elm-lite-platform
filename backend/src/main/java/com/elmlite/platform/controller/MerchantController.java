package com.elmlite.platform.controller;

import com.elmlite.platform.common.ApiResponse;
import com.elmlite.platform.entity.Merchant;
import com.elmlite.platform.service.MerchantService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/v1/merchants")
public class MerchantController {

    private final MerchantService merchantService;

    public MerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MerchantResponse> register(
            @Valid @RequestBody MerchantRegisterRequest request) {
        Merchant merchant = merchantService.register(
                request.account(),
                request.password(),
                request.merchantName(),
                request.contactName(),
                request.contactPhone()
        );

        return ApiResponse.success(new MerchantResponse(
                merchant.getId(),
                merchant.getAccount(),
                merchant.getMerchantName()
        ));
    }

    public record MerchantRegisterRequest(
            @NotBlank(message = "商家账号不能为空")
            @Size(max = 50, message = "商家账号不能超过50个字符")
            String account,
            @NotBlank(message = "密码不能为空")
            @Size(min = 8, max = 72, message = "密码长度必须为8至72位")
            String password,
            @NotBlank(message = "商家名称不能为空")
            @Size(max = 100, message = "商家名称不能超过100个字符")
            String merchantName,
            @NotBlank(message = "联系人不能为空")
            @Size(max = 50, message = "联系人不能超过50个字符")
            String contactName,
            @NotBlank(message = "联系电话不能为空")
            @Size(max = 20, message = "联系电话不能超过20个字符")
            String contactPhone
    ) {
        @Override
        public String toString() {
            return "MerchantRegisterRequest[REDACTED]";
        }
    }

    public record MerchantResponse(
            Long id,
            String account,
            String merchantName
    ) {
    }
}
