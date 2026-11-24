package com.elmlite.platform.points.domain;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PointsEarningRuleTest {

    @Test
    void calculatesWholeOrderUnitsAndExpiry() {
        var rule = new PointsEarningRule(100L, 1L, 365);
        var grant = rule.evaluate(12_345L, LocalDate.of(2026, 11, 22));

        assertEquals(123L, grant.points());
        assertEquals(LocalDate.of(2027, 11, 22), grant.expiresOn());
    }

    @Test
    void calculatesReviewOrActivityAwards() {
        var rule = new PointsEarningRule(1L, 5L, 30);
        var grant = rule.evaluate(2L, LocalDate.of(2026, 11, 22));

        assertEquals(10L, grant.points());
        assertEquals(LocalDate.of(2026, 12, 22), grant.expiresOn());
    }

    @Test
    void insufficientBasisEarnsZeroPoints() {
        var rule = new PointsEarningRule(100L, 1L, 30);

        assertEquals(0L,
                rule.evaluate(99L, LocalDate.of(2026, 11, 22)).points());
    }

    @Test
    void rejectsInvalidRulesAndNegativeBasis() {
        assertThrows(IllegalArgumentException.class,
                () -> new PointsEarningRule(0L, 1L, 30));
        assertThrows(IllegalArgumentException.class,
                () -> new PointsEarningRule(1L, 0L, 30));
        assertThrows(IllegalArgumentException.class,
                () -> new PointsEarningRule(1L, 1L, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new PointsEarningRule(1L, 1L, 36501));

        var rule = new PointsEarningRule(1L, 1L, 30);
        assertThrows(IllegalArgumentException.class,
                () -> rule.evaluate(-1L, LocalDate.of(2026, 11, 22)));
    }

    @Test
    void detectsPointOverflow() {
        var rule = new PointsEarningRule(1L, 2L, 30);

        assertThrows(ArithmeticException.class,
                () -> rule.evaluate(Long.MAX_VALUE, LocalDate.of(2026, 11, 22)));
    }
}
