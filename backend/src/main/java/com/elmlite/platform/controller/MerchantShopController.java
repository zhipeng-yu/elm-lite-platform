package com.elmlite.platform.controller;

import com.elmlite.platform.common.ApiResponse;
import com.elmlite.platform.entity.Shop;
import com.elmlite.platform.service.MerchantShopService;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/merchant/shops")
public class MerchantShopController {

    private final MerchantShopService merchantShopService;

    public MerchantShopController(MerchantShopService merchantShopService) {
        this.merchantShopService = merchantShopService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ShopResponse>> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateShopRequest request) {

        Shop shop = merchantShopService.create(
                Long.parseLong(jwt.getSubject()),
                request.shopName(),
                request.description(),
                request.address(),
                request.imageUrl(),
                request.startPriceCent(),
                request.deliveryPriceCent());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(ShopResponse.from(shop)));
    }

    @PatchMapping("/{id}")
    public ApiResponse<ShopResponse> updateBusinessStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") long id,
            @Valid @RequestBody UpdateBusinessStatusRequest request) {

        Shop shop = merchantShopService.updateBusinessStatus(
                Long.parseLong(jwt.getSubject()),
                id,
                request.businessStatus());

        return ApiResponse.success(ShopResponse.from(shop));
    }

    // 店铺归属只能来自 Token，忽略客户端提交的 merchantId。
    @JsonIgnoreProperties({"merchantId"})
    public record CreateShopRequest(
            @NotBlank(message = "店铺名称不能为空")
            @Size(max = 100, message = "店铺名称不能超过100位")
            String shopName,

            @Size(max = 255, message = "店铺描述不能超过255位")
            String description,

            @NotBlank(message = "店铺地址不能为空")
            @Size(max = 255, message = "店铺地址不能超过255位")
            String address,

            @Size(max = 255, message = "图片地址不能超过255位")
            String imageUrl,

            @NotNull(message = "起送金额不能为空")
            @Min(value = 0, message = "起送金额不能为负数")
            @Max(value = 9_999_999_999L, message = "起送金额超出上限")
            Long startPriceCent,

            @NotNull(message = "配送费不能为空")
            @Min(value = 0, message = "配送费不能为负数")
            @Max(value = 9_999_999_999L, message = "配送费超出上限")
            Long deliveryPriceCent) {
    }

    public record UpdateBusinessStatusRequest(
            @NotNull(message = "营业状态不能为空")
            @Min(value = 0, message = "营业状态必须为0、1或2")
            @Max(value = 2, message = "营业状态必须为0、1或2")
            Integer businessStatus) {
    }

    public record ShopResponse(
            Long id,
            String shopName,
            String description,
            String address,
            String imageUrl,
            Long startPriceCent,
            Long deliveryPriceCent,
            Integer businessStatus) {

        static ShopResponse from(Shop shop) {
            return new ShopResponse(
                    shop.getId(),
                    shop.getShopName(),
                    shop.getDescription(),
                    shop.getAddress(),
                    shop.getImageUrl(),
                    shop.getStartPrice().movePointRight(2).longValueExact(),
                    shop.getDeliveryPrice().movePointRight(2).longValueExact(),
                    shop.getBusinessStatus());
        }
    }
}
