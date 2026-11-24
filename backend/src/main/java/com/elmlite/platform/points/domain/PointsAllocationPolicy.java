package com.elmlite.platform.points.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class PointsAllocationPolicy {
    private PointsAllocationPolicy() { }

    // The caller supplies non-expired batches ordered by expiration, then ID.
    public static List<Allocation> allocate(List<Batch> batches, long requested) {
        Objects.requireNonNull(batches, "batches");
        if (requested <= 0) throw new IllegalArgumentException("Requested points must be positive");
        long remaining = requested;
        List<Allocation> result = new ArrayList<>();
        for (Batch batch : batches) {
            if (batch == null || batch.id() <= 0 || batch.remaining() < 0) {
                throw new IllegalArgumentException("Invalid points batch");
            }
            if (batch.remaining() == 0) continue;
            long used = Math.min(remaining, batch.remaining());
            result.add(new Allocation(batch.id(), used));
            remaining -= used;
            if (remaining == 0) return List.copyOf(result);
        }
        throw new InsufficientPointsException();
    }

    public record Batch(long id, long remaining) { }
    public record Allocation(long grantId, long points) { }
    public static final class InsufficientPointsException extends RuntimeException { }
}
