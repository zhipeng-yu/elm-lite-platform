package com.elmlite.platform.service;

import com.elmlite.platform.entity.Admin;
import com.elmlite.platform.entity.PointsRule;
import com.elmlite.platform.exception.BusinessException;
import com.elmlite.platform.mapper.AdminMapper;
import com.elmlite.platform.mapper.PointsRuleMapper;
import com.elmlite.platform.points.domain.PointsEarningRule;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class PointsRuleService {

    public static final long MAX_RULE_VALUE = 9_999_999_999L;

    private static final Set<String> SOURCES =
            Set.of("ORDER", "REVIEW", "ACTIVITY");

    private final PointsRuleMapper rules;
    private final AdminMapper admins;

    public PointsRuleService(PointsRuleMapper rules, AdminMapper admins) {
        this.rules = rules;
        this.admins = admins;
    }

    @Transactional(readOnly = true)
    public List<RuleView> listRules(long adminId) {
        requireAdmin(adminId);
        return rules.findAll().stream().map(RuleView::from).toList();
    }

    @Transactional(readOnly = true)
    public RuleView getRule(long adminId, String sourceType) {
        requireAdmin(adminId);
        return RuleView.from(requireRule(sourceType));
    }

    @Transactional
    public RuleView updateRule(
            long adminId,
            String sourceType,
            long unitsPerAward,
            long pointsPerAward,
            int validDays,
            boolean enabled) {

        requireAdmin(adminId);

        if (unitsPerAward <= 0 || unitsPerAward > MAX_RULE_VALUE
                || pointsPerAward <= 0 || pointsPerAward > MAX_RULE_VALUE
                || validDays <= 0 || validDays > 36500) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Invalid points rule");
        }

        new PointsEarningRule(unitsPerAward, pointsPerAward, validDays);

        PointsRule rule = requireRule(sourceType);
        rule.setUnitsPerAward(unitsPerAward);
        rule.setPointsPerAward(pointsPerAward);
        rule.setValidDays(validDays);
        rule.setEnabled(enabled ? 1 : 0);

        if (rules.updateById(rule) != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, "Points rule changed");
        }

        return RuleView.from(rule);
    }

    private PointsRule requireRule(String sourceType) {
        if (!SOURCES.contains(sourceType)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Points rule not found");
        }

        PointsRule rule = rules.selectById(sourceType);

        if (rule == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Points rule not found");
        }

        return rule;
    }

    private void requireAdmin(long adminId) {
        Admin admin = admins.selectById(adminId);

        if (admin == null || !Integer.valueOf(1).equals(admin.getStatus())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Administrator is unavailable");
        }
    }

    public record RuleView(
            String sourceType,
            long unitsPerAward,
            long pointsPerAward,
            int validDays,
            boolean enabled) {

        static RuleView from(PointsRule rule) {
            return new RuleView(
                    rule.getSourceType(),
                    rule.getUnitsPerAward(),
                    rule.getPointsPerAward(),
                    rule.getValidDays(),
                    Integer.valueOf(1).equals(rule.getEnabled()));
        }
    }
}
