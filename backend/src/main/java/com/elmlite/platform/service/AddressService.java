package com.elmlite.platform.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.elmlite.platform.entity.DeliveryAddress;
import com.elmlite.platform.dto.AddressRequest;
import com.elmlite.platform.exception.BusinessException;
import com.elmlite.platform.mapper.DeliveryAddressMapper;
import com.elmlite.platform.mapper.UserMapper;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDateTime;

@Service
public class AddressService {
    private final DeliveryAddressMapper addressMapper;
    private final UserService userService;
    private final UserMapper userMapper;
    private final Validator validator;

    public AddressService(DeliveryAddressMapper addressMapper, UserService userService,
                          UserMapper userMapper, Validator validator) {
        this.addressMapper = addressMapper;
        this.userService = userService;
        this.userMapper = userMapper;
        this.validator = validator;
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

    @Transactional
    public DeliveryAddress create(long userId, AddressRequest request) {
        lockUser(userId);
        DeliveryAddress address = new DeliveryAddress();
        address.setUserId(userId);
        address.setIsDefault(0);
        AddressRequest.Fields fields = request.fields(address);
        validate(fields);
        clearDefaultIfSelected(userId, fields.isDefault());
        address.setReceiverName(fields.receiverName());
        address.setReceiverPhone(fields.receiverPhone());
        address.setAddressDetail(fields.addressDetail());
        address.setAddressLabel(fields.addressLabel());
        address.setIsDefault(fields.isDefault());
        addressMapper.insert(address);
        return address;
    }

    @Transactional
    public DeliveryAddress update(long userId, long id, AddressRequest request) {
        lockUser(userId);
        DeliveryAddress address = requireOwned(userId, id);
        if (request.isEmpty()) throw new BusinessException(HttpStatus.BAD_REQUEST, "修改内容不能为空");
        AddressRequest.Fields fields = request.fields(address);
        validate(fields);
        clearDefaultIfSelected(userId, fields.isDefault());
        // 显式 set 让 addressLabel=null 能入库，不受 MyBatis-Plus 默认忽略 null 的影响。
        addressMapper.update(Wrappers.<DeliveryAddress>lambdaUpdate().eq(DeliveryAddress::getId, id)
                .set(DeliveryAddress::getReceiverName, fields.receiverName())
                .set(DeliveryAddress::getReceiverPhone, fields.receiverPhone())
                .set(DeliveryAddress::getAddressDetail, fields.addressDetail())
                .set(DeliveryAddress::getAddressLabel, fields.addressLabel())
                .set(DeliveryAddress::getIsDefault, fields.isDefault())
                .set(DeliveryAddress::getUpdatedAt, LocalDateTime.now()));
        return addressMapper.selectById(id);
    }

    @Transactional
    public void delete(long userId, long id) {
        lockUser(userId);
        requireOwned(userId, id);
        addressMapper.deleteById(id);
    }

    private void lockUser(long userId) {
        userMapper.lockById(userId);
        userService.getCurrent(userId);
    }

    private void validate(AddressRequest.Fields fields) {
        var violations = validator.validate(fields);
        if (!violations.isEmpty()) throw new ConstraintViolationException(violations);
    }

    private void clearDefaultIfSelected(long userId, int isDefault) {
        if (isDefault == 1) {
            addressMapper.update(Wrappers.<DeliveryAddress>lambdaUpdate()
                    .eq(DeliveryAddress::getUserId, userId).eq(DeliveryAddress::getIsDefault, 1)
                    .set(DeliveryAddress::getIsDefault, 0));
        }
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
