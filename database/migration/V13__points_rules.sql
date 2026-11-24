CREATE TABLE points_rule (
    source_type VARCHAR(16) NOT NULL COMMENT '积分来源类型',
    units_per_award BIGINT NOT NULL COMMENT '每次获得积分所需的基础单位',
    points_per_award BIGINT NOT NULL COMMENT '每组基础单位获得的积分',
    valid_days INT NOT NULL COMMENT '积分有效天数',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    PRIMARY KEY (source_type),
    CONSTRAINT chk_points_rule_source CHECK (source_type IN ('ORDER', 'REVIEW', 'ACTIVITY')),
    CONSTRAINT chk_points_rule_units CHECK (units_per_award > 0),
    CONSTRAINT chk_points_rule_award CHECK (points_per_award > 0),
    CONSTRAINT chk_points_rule_days CHECK (valid_days BETWEEN 1 AND 36500),
    CONSTRAINT chk_points_rule_enabled CHECK (enabled IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分获取规则';

INSERT INTO points_rule
    (source_type, units_per_award, points_per_award, valid_days, enabled)
VALUES
    ('ORDER', 100, 1, 365, 1),
    ('REVIEW', 1, 5, 365, 1),
    ('ACTIVITY', 1, 10, 365, 1);
