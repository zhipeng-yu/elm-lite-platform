package com.elmlite.platform.service;

import com.elmlite.platform.entity.Admin;
import com.elmlite.platform.entity.PointsMallItem;
import com.elmlite.platform.entity.PointsMallRedemption;
import com.elmlite.platform.exception.BusinessException;
import com.elmlite.platform.mapper.AdminMapper;
import com.elmlite.platform.mapper.PointsGrantMapper;
import com.elmlite.platform.mapper.PointsMallItemMapper;
import com.elmlite.platform.mapper.PointsMallRedemptionMapper;
import com.elmlite.platform.mapper.PointsOrderSpendMapper;
import com.elmlite.platform.points.domain.PointsAllocationPolicy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class PointsMallService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private final PointsMallItemMapper items;
    private final PointsMallRedemptionMapper redemptions;
    private final PointsGrantMapper grants;
    private final PointsOrderSpendMapper spendBatches;
    private final AdminMapper admins;
    private final UserService users;

    public PointsMallService(PointsMallItemMapper items, PointsMallRedemptionMapper redemptions,
                             PointsGrantMapper grants, PointsOrderSpendMapper spendBatches,
                             AdminMapper admins, UserService users) {
        this.items = items;
        this.redemptions = redemptions;
        this.grants = grants;
        this.spendBatches = spendBatches;
        this.admins = admins;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<ItemView> catalog(long userId) {
        users.getCurrent(userId);
        return items.findEnabled().stream().map(ItemView::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ItemView> adminItems(long adminId) {
        requireAdmin(adminId);
        return items.findAll().stream().map(ItemView::from).toList();
    }

    @Transactional
    public ItemView createItem(long adminId, String name, long pointsCost, int stock, boolean enabled) {
        requireAdmin(adminId);
        PointsMallItem item = new PointsMallItem();
        updateFields(item, name, pointsCost, stock, enabled);
        items.insert(item);
        return ItemView.from(item);
    }

    @Transactional
    public ItemView updateItem(long adminId, long itemId, String name, long pointsCost, int stock, boolean enabled) {
        requireAdmin(adminId);
        PointsMallItem item = items.lockById(itemId);
        if (item == null) throw new BusinessException(HttpStatus.NOT_FOUND, "Mall item not found");
        updateFields(item, name, pointsCost, stock, enabled);
        if (items.updateById(item) != 1) throw new BusinessException(HttpStatus.CONFLICT, "Mall item changed");
        return ItemView.from(item);
    }

    @Transactional
    public RedemptionView redeem(long userId, long itemId, int quantity, String requestKey) {
        Integer status = grants.lockUserStatus(userId);
        if (status == null) throw new BusinessException(HttpStatus.NOT_FOUND, "User not found");
        if (!Integer.valueOf(1).equals(status)) throw new BusinessException(HttpStatus.FORBIDDEN, "User is disabled");
        PointsMallRedemption existing = redemptions.lockRequest(userId, requestKey);
        if (existing != null) {
            if (existing.getItemId().longValue() != itemId || existing.getQuantity().intValue() != quantity) {
                throw new BusinessException(HttpStatus.CONFLICT, "Request key already used with different items");
            }
            return RedemptionView.from(existing);
        }
        PointsMallItem item = items.lockById(itemId);
        if (item == null) throw new BusinessException(HttpStatus.NOT_FOUND, "Mall item not found");
        if (!Integer.valueOf(1).equals(item.getEnabled()) || item.getStock() < quantity) {
            throw new BusinessException(HttpStatus.CONFLICT, "Mall item unavailable");
        }
        long total;
        try {
            total = Math.multiplyExact(item.getPointsCost(), quantity);
        } catch (ArithmeticException exception) {
            throw new BusinessException(HttpStatus.CONFLICT, "Points amount exceeds limit");
        }
        List<PointsAllocationPolicy.Batch> batches = spendBatches.lockAvailable(userId, LocalDate.now(ZONE))
                .stream().map(grant -> new PointsAllocationPolicy.Batch(grant.getId(), grant.getRemainingPoints())).toList();
        List<PointsAllocationPolicy.Allocation> plan;
        try {
            plan = PointsAllocationPolicy.allocate(batches, total);
        } catch (PointsAllocationPolicy.InsufficientPointsException exception) {
            throw new BusinessException(HttpStatus.CONFLICT, "Insufficient available points");
        }
        if (items.deductStock(itemId, quantity) != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, "Mall stock changed");
        }
        PointsMallRedemption redemption = new PointsMallRedemption();
        redemption.setUserId(userId);
        redemption.setItemId(itemId);
        redemption.setItemName(item.getName());
        redemption.setQuantity(quantity);
        redemption.setPointsPerItem(item.getPointsCost());
        redemption.setTotalPoints(total);
        redemption.setRequestKey(requestKey);
        redemption.setCreatedAt(LocalDateTime.now(ZONE).withNano(0));
        redemptions.insert(redemption);
        for (var allocation : plan) {
            if (spendBatches.deduct(allocation.grantId(), userId, allocation.points()) != 1) {
                throw new BusinessException(HttpStatus.CONFLICT, "Points balance changed");
            }
            redemptions.allocate(redemption.getId(), allocation.grantId(), allocation.points());
        }
        return RedemptionView.from(redemption);
    }

    @Transactional(readOnly = true)
    public List<RedemptionView> history(long userId) {
        users.getCurrent(userId);
        return redemptions.findForUser(userId).stream().map(RedemptionView::from).toList();
    }

    private void requireAdmin(long adminId) {
        Admin admin = admins.selectById(adminId);
        if (admin == null || !Integer.valueOf(1).equals(admin.getStatus())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Administrator is unavailable");
        }
    }

    private static void updateFields(PointsMallItem item, String name, long pointsCost, int stock, boolean enabled) {
        item.setName(name.strip());
        item.setPointsCost(pointsCost);
        item.setStock(stock);
        item.setEnabled(enabled ? 1 : 0);
    }

    public record ItemView(long id, String name, long pointsCost, int stock, boolean enabled) {
        static ItemView from(PointsMallItem item) {
            return new ItemView(item.getId(), item.getName(), item.getPointsCost(), item.getStock(),
                    Integer.valueOf(1).equals(item.getEnabled()));
        }
    }
    public record RedemptionView(long id, long itemId, String itemName, int quantity, long pointsPerItem,
                                 long totalPoints, String requestKey, LocalDateTime createdAt) {
        static RedemptionView from(PointsMallRedemption redemption) {
            return new RedemptionView(redemption.getId(), redemption.getItemId(), redemption.getItemName(),
                    redemption.getQuantity(), redemption.getPointsPerItem(), redemption.getTotalPoints(),
                    redemption.getRequestKey(), redemption.getCreatedAt());
        }
    }
}
