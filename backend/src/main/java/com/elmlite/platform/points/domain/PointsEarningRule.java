package com.elmlite.platform.points.domain;

import java.time.LocalDate;
import java.util.Objects;

public final class PointsEarningRule {

    private final long unitsPerAward;
    private final long pointsPerAward;
    private final int validDays;

    public PointsEarningRule(
            long unitsPerAward,
            long pointsPerAward,
            int validDays) {

        if (unitsPerAward <= 0 || pointsPerAward <= 0
                || validDays <= 0 || validDays > 36500) {
            throw new IllegalArgumentException("Invalid points earning rule");
        }

        this.unitsPerAward = unitsPerAward;
        this.pointsPerAward = pointsPerAward;
        this.validDays = validDays;
    }

    public Grant evaluate(long basisUnits, LocalDate earnedOn) {
        if (basisUnits < 0) {
            throw new IllegalArgumentException("Basis units cannot be negative");
        }

        Objects.requireNonNull(earnedOn, "earnedOn");

        long awards = basisUnits / unitsPerAward;
        long points = Math.multiplyExact(awards, pointsPerAward);
        LocalDate expiresOn = earnedOn.plusDays(validDays);

        return new Grant(points, expiresOn);
    }

    public record Grant(long points, LocalDate expiresOn) {
    }
}
