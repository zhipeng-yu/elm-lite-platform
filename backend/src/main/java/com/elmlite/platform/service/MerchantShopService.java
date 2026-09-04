package com.elmlite.platform.service;

import com.elmlite.platform.entity.Merchant;
import com.elmlite.platform.entity.Shop;
import com.elmlite.platform.exception.BusinessException;
import com.elmlite.platform.mapper.MerchantMapper;
import com.elmlite.platform.mapper.ShopMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class MerchantShopService {

    private final ShopMapper shopMapper;
    private final MerchantMapper merchantMapper;

    public MerchantShopService(ShopMapper shopMapper, MerchantMapper merchantMapper) {
        this.shopMapper = shopMapper;
        this.merchantMapper = merchantMapper;
    }

    @Transactional
    public Shop create(
            long merchantId,
            String shopName,
            String description,
            String address,
            String imageUrl,
            Long startPriceCent,
            Long deliveryPriceCent) {

        requireActiveMerchant(merchantId);
        checkMoney(startPriceCent);
        checkMoney(deliveryPriceCent);

        Shop shop = new Shop();
        shop.setMerchantId(merchantId);
        shop.setShopName(shopName);
        shop.setDescription(description);
        shop.setAddress(address);
        shop.setImageUrl(imageUrl);
        shop.setStartPrice(BigDecimal.valueOf(startPriceCent, 2));
        shop.setDeliveryPrice(BigDecimal.valueOf(deliveryPriceCent, 2));
        shop.setBusinessStatus(0);
        shopMapper.insert(shop);
        return shop;
    }

    @Transactional
    public Shop updateBusinessStatus(long merchantId, long shopId, Integer businessStatus) {
        requireActiveMerchant(merchantId);

        if (businessStatus == null || businessStatus < 0 || businessStatus > 2) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "营业状态无效");
        }

        Shop shop = shopMapper.selectById(shopId);
        if (shop == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "店铺不存在");
        }
        if (!Long.valueOf(merchantId).equals(shop.getMerchantId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "无权操作该店铺");
        }

        // 只更新状态及更新时间，不覆盖其他店铺字段。
        Shop update = new Shop();
        update.setId(shopId);
        update.setBusinessStatus(businessStatus);
        update.setUpdatedAt(LocalDateTime.now());
        shopMapper.updateById(update);

        shop.setBusinessStatus(businessStatus);
        shop.setUpdatedAt(update.getUpdatedAt());
        return shop;
    }

    private void requireActiveMerchant(long merchantId) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "商家账号不存在");
        }
        if (!Integer.valueOf(1).equals(merchant.getStatus())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "账号已禁用");
        }
    }

    private void checkMoney(Long cent) {
        if (cent == null || cent < 0 || cent > 9_999_999_999L) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "金额超出允许范围");
        }
    }
}
