package com.elmlite.platform.service;

import com.elmlite.platform.entity.Admin;
import com.elmlite.platform.entity.PointsGrant;
import com.elmlite.platform.entity.PointsRule;
import com.elmlite.platform.exception.BusinessException;
import com.elmlite.platform.mapper.AdminMapper;
import com.elmlite.platform.mapper.PointsGrantMapper;
import com.elmlite.platform.points.domain.PointsEarningRule;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class PointsGrantService {
    private final PointsGrantMapper grants;
    private final AdminMapper admins;
    private final UserService userService;

    public PointsGrantService(PointsGrantMapper grants, AdminMapper admins, UserService userService) {
        this.grants = grants;
        this.admins = admins;
        this.userService = userService;
    }

    @Transactional
    public GrantView grant(long adminId, long userId, String sourceType, String eventKey, long basisUnits) {
        Admin admin = admins.selectById(adminId);
        if (admin == null || !Integer.valueOf(1).equals(admin.getStatus())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Administrator is unavailable");
        }
        // Lock the user before looking up the event, serializing retries for that user.
        Integer userStatus = grants.lockUserStatus(userId);
        if (userStatus == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "User not found");
        }
        if (!Integer.valueOf(1).equals(userStatus)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "User is disabled");
        }
        PointsGrant existing = grants.findEventForUpdate(userId, sourceType, eventKey);
        if (existing != null) {
            if (existing.getBasisUnits().longValue() != basisUnits) {
                throw new BusinessException(HttpStatus.CONFLICT, "Event already used with different units");
            }
            return view(existing, today());
        }
        PointsRule rule = grants.findRuleForUpdate(sourceType);
        if (rule == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Points rule not found");
        }
        if (!Integer.valueOf(1).equals(rule.getEnabled())) {
            throw new BusinessException(HttpStatus.CONFLICT, "Points rule disabled");
        }
        LocalDate day = today();
        PointsEarningRule.Grant calculation;
        try {
            calculation = new PointsEarningRule(rule.getUnitsPerAward(), rule.getPointsPerAward(),
                    rule.getValidDays()).evaluate(basisUnits, day);
        } catch (ArithmeticException exception) {
            throw new BusinessException(HttpStatus.CONFLICT, "Points amount exceeds limit");
        }
        PointsGrant grant = new PointsGrant();
        grant.setUserId(userId);
        grant.setSourceType(sourceType);
        grant.setEventKey(eventKey);
        grant.setBasisUnits(basisUnits);
        grant.setAwardedPoints(calculation.points());
        grant.setRemainingPoints(calculation.points());
        grant.setEarnedOn(day);
        grant.setExpiresOn(calculation.expiresOn());
        grants.insert(grant);
        return view(grant, day);
    }

    @Transactional(readOnly = true)
    public BalanceView balance(long userId) {
        userService.getCurrent(userId);
        LocalDate day = today();
        long total = 0;
        for (PointsGrant grant : grants.findForUser(userId)) {
            if (day.isBefore(grant.getExpiresOn())) {
                try {
                    total = Math.addExact(total, grant.getRemainingPoints());
                } catch (ArithmeticException exception) {
                    throw new BusinessException(HttpStatus.CONFLICT, "Points balance exceeds limit");
                }
            }
        }
        return new BalanceView(total);
    }

    @Transactional(readOnly = true)
    public List<GrantView> history(long userId) {
        userService.getCurrent(userId);
        LocalDate day = today();
        return grants.findForUser(userId).stream().map(entry -> view(entry, day)).toList();
    }

    private static LocalDate today() {
        return LocalDate.now(ZoneId.of("Asia/Shanghai"));
    }

    private static GrantView view(PointsGrant grant, LocalDate day) {
        return new GrantView(grant.getId(), grant.getSourceType(), grant.getEventKey(),
                grant.getBasisUnits(), grant.getAwardedPoints(), grant.getRemainingPoints(),
                grant.getEarnedOn(), grant.getExpiresOn(), !day.isBefore(grant.getExpiresOn()));
    }

    public record BalanceView(long availablePoints) { }
    public record GrantView(long id, String sourceType, String eventKey, long basisUnits,
                            long awardedPoints, long remainingPoints, LocalDate earnedOn,
                            LocalDate expiresOn, boolean expired) { }
}
