package com.elmlite.platform.controller;

import com.elmlite.platform.common.ApiResponse;
import com.elmlite.platform.entity.Product;
import com.elmlite.platform.service.MerchantProductService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MerchantProductController {

    private final MerchantProductService merchantProductService;

    public MerchantProductController(
            MerchantProductService merchantProductService) {
        this.merchantProductService = merchantProductService;
    }

    @PostMapping("/api/v1/merchant/shops/{shopId}/products")
    public ResponseEntity<ApiResponse<ProductResponse>> create(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("shopId") long shopId,
            @Valid @RequestBody CreateProductRequest request) {

        Product product =
                merchantProductService.create(
                        Long.parseLong(jwt.getSubject()),
                        shopId,
                        request.categoryId(),
                        request.productName(),
                        request.description(),
                        request.imageUrl(),
                        request.priceCent(),
                        request.stock());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        ProductResponse.from(product)));
    }

    @PatchMapping("/api/v1/merchant/products/{id}")
    public ApiResponse<ProductResponse> update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") long id,
            @Valid @RequestBody UpdateProductRequest request) {

        Product product =
                merchantProductService.update(
                        Long.parseLong(jwt.getSubject()),
                        id,
                        request.categoryId(),
                        request.productName(),
                        request.description(),
                        request.imageUrl(),
                        request.priceCent(),
                        request.stock(),
                        request.status());

        return ApiResponse.success(
                ProductResponse.from(product));
    }

    public record CreateProductRequest(
            @NotNull(message = "分类不能为空")
            Long categoryId,

            @NotBlank(message = "商品名称不能为空")
            @Size(
                    max = 100,
                    message = "商品名称不能超过100个字符")
            String productName,

            @Size(
                    max = 255,
                    message = "商品描述不能超过255个字符")
            String description,

            @Size(
                    max = 255,
                    message = "图片地址不能超过255个字符")
            String imageUrl,

            @NotNull(message = "商品价格不能为空")
            @Min(
                    value = 1,
                    message = "商品价格必须大于0")
            @Max(
                    value = 9_999_999_999L,
                    message = "商品价格超出上限")
            Long priceCent,

            @NotNull(message = "商品库存不能为空")
            @Min(
                    value = 0,
                    message = "商品库存不能为负数")
            Integer stock) {
    }

    public record UpdateProductRequest(
            Long categoryId,

            @Size(
                    max = 100,
                    message = "商品名称不能超过100个字符")
            String productName,

            @Size(
                    max = 255,
                    message = "商品描述不能超过255个字符")
            String description,

            @Size(
                    max = 255,
                    message = "图片地址不能超过255个字符")
            String imageUrl,

            @Min(
                    value = 1,
                    message = "商品价格必须大于0")
            @Max(
                    value = 9_999_999_999L,
                    message = "商品价格超出上限")
            Long priceCent,

            @Min(
                    value = 0,
                    message = "商品库存不能为负数")
            Integer stock,

            @Min(
                    value = 0,
                    message = "商品状态必须为0或1")
            @Max(
                    value = 1,
                    message = "商品状态必须为0或1")
            Integer status) {
    }

    public record ProductResponse(
            Long id,
            Long shopId,
            Long categoryId,
            String productName,
            String description,
            String imageUrl,
            Long priceCent,
            Integer stock,
            Integer status) {

        static ProductResponse from(Product product) {
            return new ProductResponse(
                    product.getId(),
                    product.getShopId(),
                    product.getCategoryId(),
                    product.getProductName(),
                    product.getDescription(),
                    product.getImageUrl(),
                    product.getPrice()
                            .movePointRight(2)
                            .longValueExact(),
                    product.getStock(),
                    product.getStatus());
        }
    }
}