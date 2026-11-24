package com.elmlite.platform.service;

import com.elmlite.platform.entity.Order;
import com.elmlite.platform.entity.PointsGrant;
import com.elmlite.platform.entity.PointsRule;
import com.elmlite.platform.exception.BusinessException;
import com.elmlite.platform.mapper.PointsGrantMapper;
import com.elmlite.platform.points.domain.PointsEarningRule;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class OrderPointsAwardService {
    private final PointsGrantMapper grants;

    public OrderPointsAwardService(PointsGrantMapper grants) {
        this.grants = grants;
    }

    // The caller holds the user and order locks and owns the order transaction.
    @Transactional(propagation = Propagation.MANDATORY)
    public void awardCompletedOrder(Order order) {
        BigDecimal paidProducts = order.getProductAmount().subtract(order.getDiscountAmount());
        if (paidProducts.signum() < 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "Invalid order discount");
        }
        long basisUnits;
        try {
            basisUnits = paidProducts.movePointRight(2).longValueExact();
        } catch (ArithmeticException exception) {
            throw new BusinessException(HttpStatus.CONFLICT, "Order amount exceeds points limit");
        }
        String eventKey = "order:" + order.getId();
        PointsGrant existing = grants.findEventForUpdate(order.getUserId(), "ORDER", eventKey);
        if (existing != null) {
            if (existing.getBasisUnits().longValue() != basisUnits) {
                throw new BusinessException(HttpStatus.CONFLICT, "Order points event conflicts");
            }
            return;
        }
        PointsRule rule = grants.findRuleForUpdate("ORDER");
        if (rule == null || !Integer.valueOf(1).equals(rule.getEnabled())) return;

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        PointsEarningRule.Grant award;
        try {
            award = new PointsEarningRule(rule.getUnitsPerAward(), rule.getPointsPerAward(),
                    rule.getValidDays()).evaluate(basisUnits, today);
        } catch (ArithmeticException exception) {
            throw new BusinessException(HttpStatus.CONFLICT, "Points amount exceeds limit");
        }
        PointsGrant entry = new PointsGrant();
        entry.setUserId(order.getUserId());
        entry.setSourceType("ORDER");
        entry.setEventKey(eventKey);
        entry.setBasisUnits(basisUnits);
        entry.setAwardedPoints(award.points());
        entry.setRemainingPoints(award.points());
        entry.setEarnedOn(today);
        entry.setExpiresOn(award.expiresOn());
        grants.insert(entry);
    }
}
