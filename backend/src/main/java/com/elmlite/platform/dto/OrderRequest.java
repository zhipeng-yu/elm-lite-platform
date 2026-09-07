package com.elmlite.platform.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public record OrderRequest(
        @NotNull(message = "请选择收货地址") @Positive(message = "地址ID必须为正整数") Long addressId,
        @NotEmpty(message = "请选择购物车商品") List<@NotNull @Positive Long> cartItemIds,
        @Size(max = 255, message = "备注不能超过255位") String remark) {

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static OrderRequest from(JsonNode body) {
        if (!body.isObject()) throw new IllegalArgumentException("订单请求必须为对象");
        body.fieldNames().forEachRemaining(name -> {
            if (!Set.of("addressId", "cartItemIds", "remark").contains(name)) {
                throw new IllegalArgumentException("不支持的订单字段");
            }
        });
        List<Long> ids = null;
        JsonNode selected = body.get("cartItemIds");
        if (selected != null && !selected.isNull()) {
            if (!selected.isArray()) throw new IllegalArgumentException("购物车项必须为数组");
            ids = new ArrayList<>();
            for (JsonNode id : selected) ids.add(id(id));
        }
        JsonNode remark = body.get("remark");
        if (remark != null && !remark.isNull() && !remark.isTextual()) {
            throw new IllegalArgumentException("备注必须为字符串");
        }
        String text = remark == null || remark.isNull() ? null : remark.textValue().strip();
        return new OrderRequest(id(body.get("addressId")), ids, text == null || text.isEmpty() ? null : text);
    }

    private static Long id(JsonNode value) {
        if (value == null || value.isNull()) return null;
        if (!value.isIntegralNumber() || !value.canConvertToLong()) {
            throw new IllegalArgumentException("ID必须为Long范围内的整数");
        }
        return value.longValue();
    }
}
