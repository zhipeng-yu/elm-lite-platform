package com.elmlite.platform.points.domain;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static com.elmlite.platform.points.domain.PointsAllocationPolicy.*;

class PointsAllocationPolicyTest {
    @Test
    void allocatesInTheProvidedExpirationOrder() {
        assertEquals(List.of(new Allocation(7, 10), new Allocation(2, 15)),
                allocate(List.of(new Batch(7, 10), new Batch(2, 40), new Batch(3, 50)), 25));
    }

    @Test
    void handlesExactAmountsZeroBatchesAndLargeValues() {
        assertEquals(List.of(new Allocation(2, 5)), allocate(List.of(new Batch(1, 0), new Batch(2, 5)), 5));
        assertEquals(List.of(new Allocation(1, Long.MAX_VALUE)), allocate(List.of(new Batch(1, Long.MAX_VALUE)), Long.MAX_VALUE));
    }

    @Test
    void rejectsInvalidInputsAndInsufficientBalance() {
        assertThrows(IllegalArgumentException.class, () -> allocate(List.of(), 0));
        assertThrows(IllegalArgumentException.class, () -> allocate(List.of(), -1));
        assertThrows(IllegalArgumentException.class, () -> allocate(List.of(new Batch(0, 1)), 1));
        assertThrows(IllegalArgumentException.class, () -> allocate(List.of(new Batch(1, -1)), 1));
        assertThrows(InsufficientPointsException.class, () -> allocate(List.of(new Batch(1, 10)), 11));
        assertThrows(InsufficientPointsException.class, () -> allocate(List.of(), 1));
    }
}
