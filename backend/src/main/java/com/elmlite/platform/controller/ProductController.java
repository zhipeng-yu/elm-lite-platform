package com.elmlite.platform.controller;

import com.elmlite.platform.common.ApiResponse;
import com.elmlite.platform.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/api/v1/shops/{shopId}/categories")
    public ApiResponse<List<ProductService.CategoryResponse>> listCategories(
            @PathVariable("shopId") long shopId) {

        return ApiResponse.success(productService.listCategories(shopId));
    }

    @GetMapping("/api/v1/shops/{shopId}/products")
    public ApiResponse<List<ProductService.ProductListResponse>> listProducts(
            @PathVariable("shopId") long shopId,
            @RequestParam(value = "categoryId", required = false) Long categoryId) {

        return ApiResponse.success(
                productService.listProducts(shopId, categoryId));
    }

    @GetMapping("/api/v1/products/{id}")
    public ApiResponse<ProductService.ProductDetailResponse> getProduct(
            @PathVariable("id") long id) {

        return ApiResponse.success(productService.getProduct(id));
    }
}