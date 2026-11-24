DROP TABLE IF EXISTS points_rule;

CREATE TABLE points_rule (
    source_type VARCHAR(16) PRIMARY KEY,
    units_per_award BIGINT NOT NULL,
    points_per_award BIGINT NOT NULL,
    valid_days INT NOT NULL,
    enabled TINYINT NOT NULL,
    CONSTRAINT chk_test_points_units CHECK (units_per_award > 0),
    CONSTRAINT chk_test_points_award CHECK (points_per_award > 0),
    CONSTRAINT chk_test_points_days CHECK (valid_days BETWEEN 1 AND 36500),
    CONSTRAINT chk_test_points_enabled CHECK (enabled IN (0, 1))
);

INSERT INTO admin_account (id, username, password_hash, status)
VALUES (1, 'points_admin', 'test_hash', 1);

INSERT INTO points_rule
    (source_type, units_per_award, points_per_award, valid_days, enabled)
VALUES
    ('ORDER', 100, 1, 365, 1),
    ('REVIEW', 1, 5, 365, 1),
    ('ACTIVITY', 1, 10, 365, 1);
