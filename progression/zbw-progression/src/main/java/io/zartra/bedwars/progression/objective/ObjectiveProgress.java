package io.zartra.bedwars.progression.objective;

import io.zartra.bedwars.api.identity.IdempotencyKey;
import java.time.Instant;
import java.util.Objects;

/** Immutable monotonic progress snapshot for one objective owner. */
public final class ObjectiveProgress {
    private final ObjectiveId objectiveId;
    private final String ownerId;
    private final long value;
    private final long revision;
    private final IdempotencyKey lastEvent;
    private final Instant updatedAt;

    /** Creates a validated progress snapshot. */
    public ObjectiveProgress(final ObjectiveId objectiveId, final String ownerId, final long value,
                             final long revision, final IdempotencyKey lastEvent, final Instant updatedAt) {
        this.objectiveId = Objects.requireNonNull(objectiveId, "objectiveId");
        if (ownerId == null || ownerId.trim().isEmpty() || ownerId.length() > 128) {
            throw new IllegalArgumentException("ownerId must contain 1..128 characters");
        }
        if (value < 0 || revision < 0) {
            throw new IllegalArgumentException("value and revision must not be negative");
        }
        this.ownerId = ownerId;
        this.value = value;
        this.revision = revision;
        this.lastEvent = Objects.requireNonNull(lastEvent, "lastEvent");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    /** @return objective identity */ public ObjectiveId objectiveId() { return objectiveId; }
    /** @return typed-by-scope owner identity */ public String ownerId() { return ownerId; }
    /** @return accumulated monotonic value */ public long value() { return value; }
    /** @return optimistic revision */ public long revision() { return revision; }
    /** @return last applied event key */ public IdempotencyKey lastEvent() { return lastEvent; }
    /** @return last update time */ public Instant updatedAt() { return updatedAt; }

    /** Applies a positive delta once and clamps the result to the definition target. */
    public ObjectiveProgress apply(final long delta, final IdempotencyKey eventKey,
                                   final Instant now, final long target) {
        if (delta < 1 || target < 1) {
            throw new IllegalArgumentException("delta and target must be positive");
        }
        Objects.requireNonNull(eventKey, "eventKey");
        Objects.requireNonNull(now, "now");
        if (lastEvent.equals(eventKey)) {
            return this;
        }
        final long remaining = Math.max(0L, target - value);
        final long increment = Math.min(delta, remaining);
        return new ObjectiveProgress(objectiveId, ownerId, value + increment,
                Math.addExact(revision, 1L), eventKey, now);
    }
}
