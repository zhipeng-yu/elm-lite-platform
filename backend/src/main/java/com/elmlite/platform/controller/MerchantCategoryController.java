package com.elmlite.platform.controller;

import com.elmlite.platform.common.ApiResponse;
import com.elmlite.platform.entity.ProductCategory;
import com.elmlite.platform.service.MerchantCategoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
public class MerchantCategoryController {

    private final MerchantCategoryService merchantCategoryService;

    public MerchantCategoryController(
            MerchantCategoryService merchantCategoryService) {
        this.merchantCategoryService = merchantCategoryService;
    }

    @PostMapping("/api/v1/merchant/shops/{shopId}/categories")
    public ResponseEntity<ApiResponse<CategoryResponse>> create(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("shopId") long shopId,
            @Valid @RequestBody CreateCategoryRequest request) {

        ProductCategory category =
                merchantCategoryService.create(
                        Long.parseLong(jwt.getSubject()),
                        shopId,
                        request.categoryName(),
                        request.sortOrder());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(CategoryResponse.from(category)));
    }

    @PatchMapping("/api/v1/merchant/categories/{id}")
    public ApiResponse<CategoryResponse> update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") long id,
            @Valid @RequestBody UpdateCategoryRequest request) {

        ProductCategory category =
                merchantCategoryService.update(
                        Long.parseLong(jwt.getSubject()),
                        id,
                        request.categoryName(),
                        request.sortOrder(),
                        request.status());

        return ApiResponse.success(CategoryResponse.from(category));
    }

    public record CreateCategoryRequest(
            @NotBlank(message = "分类名称不能为空")
            @Size(max = 50, message = "分类名称不能超过50个字符")
            String categoryName,

            Integer sortOrder) {
    }

    public record UpdateCategoryRequest(
            @Size(max = 50, message = "分类名称不能超过50个字符")
            String categoryName,

            Integer sortOrder,

            @Min(value = 0, message = "分类状态必须为0或1")
            @Max(value = 1, message = "分类状态必须为0或1")
            Integer status) {
    }

    public record CategoryResponse(
            Long id,
            Long shopId,
            String categoryName,
            Integer sortOrder,
            Integer status) {

        static CategoryResponse from(ProductCategory category) {
            return new CategoryResponse(
                    category.getId(),
                    category.getShopId(),
                    category.getCategoryName(),
                    category.getSortOrder(),
                    category.getStatus());
        }
    }
}
