package com.elmlite.platform.controller;

import com.elmlite.platform.common.ApiResponse;
import com.elmlite.platform.service.ShopService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/shops")
public class ShopController {

    private final ShopService shopService;

    public ShopController(ShopService shopService) {
        this.shopService = shopService;
    }

    @GetMapping
    public ApiResponse<List<ShopService.ShopResponse>> list() {
        return ApiResponse.success(shopService.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<ShopService.ShopResponse> get(@PathVariable("id") long id) {
        return ApiResponse.success(shopService.get(id));
    }
}
