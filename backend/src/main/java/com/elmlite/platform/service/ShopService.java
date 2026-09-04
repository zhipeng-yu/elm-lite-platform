package com.elmlite.platform.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.elmlite.platform.entity.Shop;
import com.elmlite.platform.exception.BusinessException;
import com.elmlite.platform.mapper.ShopMapper;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShopService {

    private final ShopMapper shopMapper;

    public ShopService(ShopMapper shopMapper) {
        this.shopMapper = shopMapper;
    }

    public List<ShopResponse> list() {
        return shopMapper.selectList(Wrappers.<Shop>lambdaQuery().orderByAsc(Shop::getId))
                .stream().map(shop -> ShopResponse.from(shop, null)).toList();
    }

    public ShopResponse get(long id) {
        Shop shop = shopMapper.selectById(id);
        if (shop == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "店铺不存在");
        }
        return ShopResponse.from(shop, shop.getAddress());
    }

    public record ShopResponse(
            Long id,
            String shopName,
            String description,
            String imageUrl,
            Long startPriceCent,
            Long deliveryPriceCent,
            Integer businessStatus,
            @JsonInclude(JsonInclude.Include.NON_NULL) String address) {

        static ShopResponse from(Shop shop, String address) {
            return new ShopResponse(shop.getId(), shop.getShopName(), shop.getDescription(),
                    shop.getImageUrl(), shop.getStartPrice().movePointRight(2).longValueExact(),
                    shop.getDeliveryPrice().movePointRight(2).longValueExact(),
                    shop.getBusinessStatus(), address);
        }
    }
}
