package com.elmlite.platform.service;

import com.elmlite.platform.entity.Admin;
import com.elmlite.platform.entity.WalletWithdrawalRule;
import com.elmlite.platform.exception.BusinessException;
import com.elmlite.platform.mapper.AdminMapper;
import com.elmlite.platform.mapper.WalletWithdrawalRuleMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletWithdrawalRuleService {

    private final WalletWithdrawalRuleMapper rules;
    private final AdminMapper admins;

    public WalletWithdrawalRuleService(
            WalletWithdrawalRuleMapper rules,
            AdminMapper admins) {
        this.rules = rules;
        this.admins = admins;
    }

    @Transactional(readOnly = true)
    public int currentFeeBasisPoints() {
        return requireRule().getFeeBasisPoints();
    }

    @Transactional(readOnly = true)
    public RuleView getRule(long adminId) {
        requireAdmin(adminId);
        return RuleView.from(requireRule());
    }

    @Transactional
    public RuleView updateRule(long adminId, int feeBasisPoints) {
        requireAdmin(adminId);

        if (feeBasisPoints < 0 || feeBasisPoints >= 10000) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Invalid withdrawal rate");
        }

        WalletWithdrawalRule rule = requireRule();
        rule.setFeeBasisPoints(feeBasisPoints);

        if (rules.updateById(rule) != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, "Withdrawal rule changed");
        }

        return RuleView.from(rule);
    }

    private WalletWithdrawalRule requireRule() {
        WalletWithdrawalRule rule = rules.selectById(1L);

        if (rule == null) {
            throw new BusinessException(HttpStatus.CONFLICT, "Withdrawal rule is missing");
        }

        return rule;
    }

    private void requireAdmin(long adminId) {
        Admin admin = admins.selectById(adminId);

        if (admin == null || !Integer.valueOf(1).equals(admin.getStatus())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Administrator is unavailable");
        }
    }

    public record RuleView(int feeBasisPoints) {
        static RuleView from(WalletWithdrawalRule rule) {
            return new RuleView(rule.getFeeBasisPoints());
        }
    }
}
