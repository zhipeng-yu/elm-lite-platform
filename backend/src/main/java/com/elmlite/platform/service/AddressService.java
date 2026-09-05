package com.elmlite.platform.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.elmlite.platform.entity.DeliveryAddress;
import com.elmlite.platform.exception.BusinessException;
import com.elmlite.platform.mapper.DeliveryAddressMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AddressService {
    private final DeliveryAddressMapper addressMapper;
    private final UserService userService;

    public AddressService(DeliveryAddressMapper addressMapper, UserService userService) {
        this.addressMapper = addressMapper;
        this.userService = userService;
    }

    public List<DeliveryAddress> list(long userId) {
        userService.getCurrent(userId);
        return addressMapper.selectList(Wrappers.<DeliveryAddress>lambdaQuery()
                .eq(DeliveryAddress::getUserId, userId)
                .orderByDesc(DeliveryAddress::getIsDefault, DeliveryAddress::getId));
    }

    public DeliveryAddress get(long userId, long id) {
        userService.getCurrent(userId);
        return requireOwned(userId, id);
    }

    private DeliveryAddress requireOwned(long userId, long id) {
        DeliveryAddress address = addressMapper.selectById(id);
        if (address == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "地址不存在");
        }
        if (!Long.valueOf(userId).equals(address.getUserId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "无权操作该地址");
        }
        return address;
    }
}
