package com.elmlite.platform.controller;

import com.elmlite.platform.common.ApiResponse;
import com.elmlite.platform.entity.DeliveryAddress;
import com.elmlite.platform.dto.AddressRequest;
import com.elmlite.platform.service.AddressService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

@RestController
@RequestMapping("/api/v1/addresses")
public class AddressController {
    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public ApiResponse<List<AddressResponse>> list(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(addressService.list(Long.parseLong(jwt.getSubject()))
                .stream().map(AddressResponse::from).toList());
    }

    @GetMapping("/{id}")
    public ApiResponse<AddressResponse> get(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") long id) {
        return ApiResponse.success(AddressResponse.from(addressService.get(Long.parseLong(jwt.getSubject()), id)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AddressResponse> create(@AuthenticationPrincipal Jwt jwt, @RequestBody AddressRequest request) {
        return ApiResponse.success(AddressResponse.from(addressService.create(Long.parseLong(jwt.getSubject()), request)));
    }

    @PatchMapping("/{id}")
    public ApiResponse<AddressResponse> update(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") long id,
                                               @RequestBody AddressRequest request) {
        return ApiResponse.success(AddressResponse.from(addressService.update(Long.parseLong(jwt.getSubject()), id, request)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") long id) {
        addressService.delete(Long.parseLong(jwt.getSubject()), id);
        return ApiResponse.success(null);
    }

    public record AddressResponse(Long id, String receiverName, String receiverPhone,
                                  String addressDetail, String addressLabel, Integer isDefault) {
        static AddressResponse from(DeliveryAddress address) {
            return new AddressResponse(address.getId(), address.getReceiverName(), address.getReceiverPhone(),
                    address.getAddressDetail(), address.getAddressLabel(), address.getIsDefault());
        }
    }
}
