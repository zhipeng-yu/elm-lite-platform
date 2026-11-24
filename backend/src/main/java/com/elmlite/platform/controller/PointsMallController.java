package com.elmlite.platform.controller;

import com.elmlite.platform.common.ApiResponse;
import com.elmlite.platform.exception.BusinessException;
import com.elmlite.platform.service.PointsMallService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
public class PointsMallController {
    private final PointsMallService service;
    public PointsMallController(PointsMallService service) { this.service = service; }

    @GetMapping("/api/v1/points/mall/items")
    public ApiResponse<List<PointsMallService.ItemView>> catalog(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(service.catalog(subject(jwt, "USER")));
    }

    @GetMapping("/api/v1/points/mall/redemptions")
    public ApiResponse<List<PointsMallService.RedemptionView>> history(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(service.history(subject(jwt, "USER")));
    }

    @PostMapping("/api/v1/points/mall/redemptions")
    public ApiResponse<PointsMallService.RedemptionView> redeem(@AuthenticationPrincipal Jwt jwt,
                                                                @Valid @RequestBody RedeemRequest request) {
        return ApiResponse.success(service.redeem(subject(jwt, "USER"), request.itemId(), request.quantity(), request.requestKey()));
    }

    @GetMapping("/api/v1/admin/points/mall/items")
    public ApiResponse<List<PointsMallService.ItemView>> adminItems(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(service.adminItems(subject(jwt, "ADMIN")));
    }

    @PostMapping("/api/v1/admin/points/mall/items")
    public ApiResponse<PointsMallService.ItemView> create(@AuthenticationPrincipal Jwt jwt,
                                                          @Valid @RequestBody ItemRequest request) {
        return ApiResponse.success(service.createItem(subject(jwt, "ADMIN"), request.name(),
                request.pointsCost(), request.stock(), request.enabled()));
    }

    @PutMapping("/api/v1/admin/points/mall/items/{id}")
    public ApiResponse<PointsMallService.ItemView> update(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") long id,
                                                          @Valid @RequestBody ItemRequest request) {
        return ApiResponse.success(service.updateItem(subject(jwt, "ADMIN"), id, request.name(),
                request.pointsCost(), request.stock(), request.enabled()));
    }

    private static long subject(Jwt jwt, String role) {
        if (!role.equals(jwt.getClaimAsString("accountType"))) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Account type not allowed");
        }
        return Long.parseLong(jwt.getSubject());
    }

    public record RedeemRequest(@NotNull @Positive Long itemId,
                                @NotNull @Min(1) @Max(100) Integer quantity,
                                @NotBlank @Pattern(regexp = "[A-Za-z0-9_.:-]{1,64}") String requestKey) { }
    public record ItemRequest(@NotBlank @Size(max = 100) String name,
                              @NotNull @Positive @Max(9_999_999_999L) Long pointsCost,
                              @NotNull @Min(0) @Max(1_000_000) Integer stock,
                              @NotNull Boolean enabled) { }
}
