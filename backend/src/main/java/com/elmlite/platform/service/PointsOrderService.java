package com.elmlite.platform.service;

import com.elmlite.platform.entity.PointsOrderAllocation;
import com.elmlite.platform.entity.PointsOrderSpend;
import com.elmlite.platform.exception.BusinessException;
import com.elmlite.platform.mapper.PointsOrderSpendMapper;
import com.elmlite.platform.points.domain.PointsAllocationPolicy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class PointsOrderService {
    private final PointsOrderSpendMapper spends;
    private final UserService users;

    public PointsOrderService(PointsOrderSpendMapper spends, UserService users) {
        this.spends = spends;
        this.users = users;
    }

    // The order service already holds the user row lock and owns the transaction.
    @Transactional(propagation = Propagation.MANDATORY)
    public void consumeForOrder(long userId, long orderId, long points) {
        PointsOrderSpend entry = new PointsOrderSpend();
        entry.setUserId(userId);
        entry.setOrderId(orderId);
        entry.setPoints(points);
        entry.setStatus("SPENT");
        spends.insert(entry);
        LocalDate day = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        List<PointsAllocationPolicy.Batch> batches = spends.lockAvailable(userId, day).stream()
                .map(grant -> new PointsAllocationPolicy.Batch(grant.getId(), grant.getRemainingPoints())).toList();
        List<PointsAllocationPolicy.Allocation> plan;
        try {
            plan = PointsAllocationPolicy.allocate(batches, points);
        } catch (PointsAllocationPolicy.InsufficientPointsException exception) {
            throw new BusinessException(HttpStatus.CONFLICT, "Insufficient available points");
        }
        for (var allocation : plan) {
            if (spends.deduct(allocation.grantId(), userId, allocation.points()) != 1) {
                throw new BusinessException(HttpStatus.CONFLICT, "Points balance changed");
            }
            spends.allocate(entry.getId(), allocation.grantId(), allocation.points());
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void returnForOrder(long userId, long orderId) {
        PointsOrderSpend entry = spends.lockForOrder(orderId);
        if (entry == null) return;
        if (!Long.valueOf(userId).equals(entry.getUserId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Points spend belongs to another user");
        }
        if ("RETURNED".equals(entry.getStatus())) return;
        long remainingToReturn = entry.getPoints();
        for (PointsOrderAllocation allocation : spends.allocations(entry.getId())) {
            if (allocation.getPoints() <= 0 || allocation.getPoints() > remainingToReturn) {
                throw new BusinessException(HttpStatus.CONFLICT, "Points allocation is invalid");
            }
            if (spends.restore(allocation.getGrantId(), userId, allocation.getPoints()) != 1) {
                throw new BusinessException(HttpStatus.CONFLICT, "Could not restore points");
            }
            remainingToReturn -= allocation.getPoints();
        }
        if (remainingToReturn != 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "Points allocation is incomplete");
        }
        if (spends.markReturned(entry.getId()) != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, "Points spend state changed");
        }
    }

    @Transactional(readOnly = true)
    public List<SpendView> history(long userId) {
        users.getCurrent(userId);
        return spends.findForUser(userId).stream().map(SpendView::from).toList();
    }

    public record SpendView(long id, long orderId, long points, String status, LocalDateTime createdAt) {
        static SpendView from(PointsOrderSpend spend) {
            return new SpendView(spend.getId(), spend.getOrderId(), spend.getPoints(),
                    spend.getStatus(), spend.getCreatedAt());
        }
    }
}
