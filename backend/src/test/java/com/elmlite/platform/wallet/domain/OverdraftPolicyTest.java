package com.elmlite.platform.wallet.domain;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class OverdraftPolicyTest {

    @Test
    void borrowsWithinRemainingLimit() {
        var policy = new OverdraftPolicy(5000L, 7, 5);

        assertEquals(3000L, policy.availableCreditCent(2000L));
        assertEquals(5000L, policy.borrow(2000L, 3000L));

        assertThrows(IllegalStateException.class,
                () -> policy.borrow(2000L, 3001L));
    }

    @Test
    void existingDebtAboveLimitCannotBorrowMore() {
        var policy = new OverdraftPolicy(5000L, 7, 5);

        assertEquals(0L, policy.availableCreditCent(6000L));
        assertThrows(IllegalStateException.class,
                () -> policy.borrow(6000L, 1L));
    }

    @Test
    void repaymentReducesPrincipal() {
        var policy = new OverdraftPolicy(5000L, 7, 5);

        assertEquals(3000L, policy.repay(5000L, 2000L));
        assertThrows(IllegalArgumentException.class,
                () -> policy.repay(5000L, 5001L));
    }

    @Test
    void calculatesDueDateFromTerm() {
        var policy = new OverdraftPolicy(5000L, 7, 5);

        assertEquals(LocalDate.of(2026, 11, 29),
                policy.dueDate(LocalDate.of(2026, 11, 22)));
    }

    @Test
    void noInterestBeforeOrOnDueDate() {
        var policy = new OverdraftPolicy(5000L, 7, 5);
        LocalDate due = LocalDate.of(2026, 11, 29);

        assertEquals(0L, policy.interestCent(10_000L, due, due.minusDays(1)));
        assertEquals(0L, policy.interestCent(10_000L, due, due));
    }

    @Test
    void calculatesSimpleInterestAfterDueDate() {
        var policy = new OverdraftPolicy(20_000L, 7, 5);
        LocalDate due = LocalDate.of(2026, 11, 29);

        assertEquals(15L,
                policy.interestCent(10_000L, due, due.plusDays(3)));
    }

    @Test
    void roundsFractionalInterestUpToOneCent() {
        var policy = new OverdraftPolicy(5000L, 7, 5);
        LocalDate due = LocalDate.of(2026, 11, 29);

        assertEquals(1L,
                policy.interestCent(1001L, due, due.plusDays(1)));
    }

    @Test
    void rejectsInvalidInputs() {
        assertThrows(IllegalArgumentException.class,
                () -> new OverdraftPolicy(-1L, 7, 5));
        assertThrows(IllegalArgumentException.class,
                () -> new OverdraftPolicy(1000L, -1, 5));
        assertThrows(IllegalArgumentException.class,
                () -> new OverdraftPolicy(1000L, 7, 10001));

        var policy = new OverdraftPolicy(1000L, 7, 5);
        assertThrows(IllegalArgumentException.class,
                () -> policy.borrow(0L, 0L));
        assertThrows(IllegalArgumentException.class,
                () -> policy.repay(-1L, 1L));
    }

    @Test
    void detectsInterestOverflow() {
        var policy = new OverdraftPolicy(Long.MAX_VALUE, 1, 10000);
        LocalDate due = LocalDate.of(2026, 1, 1);

        assertThrows(ArithmeticException.class,
                () -> policy.interestCent(
                        Long.MAX_VALUE, due, due.plusDays(2)));
    }
}
