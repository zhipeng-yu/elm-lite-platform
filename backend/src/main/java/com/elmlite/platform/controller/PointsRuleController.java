package com.elmlite.platform.controller;

import com.elmlite.platform.common.ApiResponse;
import com.elmlite.platform.service.PointsRuleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/points/rules")
public class PointsRuleController {

    private final PointsRuleService service;

    public PointsRuleController(PointsRuleService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<PointsRuleService.RuleView>> list(
            @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(
                service.listRules(Long.parseLong(jwt.getSubject())));
    }

    @GetMapping("/{sourceType}")
    public ApiResponse<PointsRuleService.RuleView> get(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String sourceType) {
        return ApiResponse.success(
                service.getRule(Long.parseLong(jwt.getSubject()), sourceType));
    }

    @PutMapping("/{sourceType}")
    public ApiResponse<PointsRuleService.RuleView> update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String sourceType,
            @Valid @RequestBody RuleRequest request) {

        return ApiResponse.success(service.updateRule(
                Long.parseLong(jwt.getSubject()),
                sourceType,
                request.unitsPerAward(),
                request.pointsPerAward(),
                request.validDays(),
                request.enabled()));
    }

    public record RuleRequest(
            @NotNull @Positive @Max(PointsRuleService.MAX_RULE_VALUE) Long unitsPerAward,
            @NotNull @Positive @Max(PointsRuleService.MAX_RULE_VALUE) Long pointsPerAward,
            @NotNull @Positive @Max(36500) Integer validDays,
            @NotNull Boolean enabled) {
    }
}
