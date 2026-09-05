package com.elmlite.platform.dto;

import com.elmlite.platform.entity.DeliveryAddress;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

public class AddressRequest {
    private static final Set<String> WRITABLE = Set.of(
            "receiverName", "receiverPhone", "addressDetail", "addressLabel", "isDefault");
    private final JsonNode body;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public AddressRequest(JsonNode body) {
        if (!body.isObject()) throw new IllegalArgumentException("地址请求必须为对象");
        body.fields().forEachRemaining(field -> {
            String name = field.getKey();
            JsonNode value = field.getValue();
            if (!WRITABLE.contains(name)) throw new IllegalArgumentException("不支持的地址字段");
            if (!value.isNull() && (name.equals("isDefault")
                    ? !value.isIntegralNumber() || !value.canConvertToInt() : !value.isTextual())) {
                throw new IllegalArgumentException("地址字段类型错误");
            }
        });
        this.body = body;
    }

    public boolean isEmpty() {
        return body.isEmpty();
    }

    public Fields fields(DeliveryAddress current) {
        String label = text("addressLabel", current.getAddressLabel());
        Integer isDefault = current.getIsDefault();
        if (body.has("isDefault")) {
            isDefault = body.get("isDefault").isNull() ? null : body.get("isDefault").intValue();
        }
        return new Fields(text("receiverName", current.getReceiverName()),
                text("receiverPhone", current.getReceiverPhone()), text("addressDetail", current.getAddressDetail()),
                label == null || label.isEmpty() ? null : label, isDefault);
    }

    private String text(String field, String current) {
        // PATCH 省略字段保留原值，显式 null 则交给校验或清空标签。
        if (!body.has(field)) return current;
        JsonNode value = body.get(field);
        return value.isNull() ? null : value.textValue().strip();
    }

    public record Fields(
            @NotBlank(message = "收货人不能为空") @Size(max = 50, message = "收货人不能超过50位") String receiverName,
            @NotBlank(message = "联系电话不能为空")
            @Pattern(regexp = "^1[0-9]{10}$", message = "联系电话必须为1开头的11位数字") String receiverPhone,
            @NotBlank(message = "详细地址不能为空") @Size(max = 255, message = "详细地址不能超过255位") String addressDetail,
            @Size(max = 20, message = "地址标签不能超过20位") String addressLabel,
            @NotNull(message = "默认标记不能为空") @Min(value = 0, message = "默认标记必须为0或1")
            @Max(value = 1, message = "默认标记必须为0或1") Integer isDefault) {
    }
}
