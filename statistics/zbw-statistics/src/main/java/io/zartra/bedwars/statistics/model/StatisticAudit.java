package io.zartra.bedwars.statistics.model;

import io.zartra.bedwars.api.identity.CorrelationId;
import java.time.Instant;
import java.util.Objects;

/** Immutable, sanitized audit metadata for a statistic definition or aggregate. */
public final class StatisticAudit {
    private final String actor;
    private final CorrelationId correlationId;
    private final Instant recordedAt;

    /** Creates audit metadata with a bounded actor label. */
    public StatisticAudit(final String actor, final CorrelationId correlationId,
                          final Instant recordedAt) {
        if (actor == null || actor.trim().isEmpty() || actor.length() > 128) {
            throw new IllegalArgumentException("actor must contain 1..128 characters");
        }
        this.actor = actor;
        this.correlationId = Objects.requireNonNull(correlationId, "correlationId");
        this.recordedAt = Objects.requireNonNull(recordedAt, "recordedAt");
    }

    /** @return accountable source label */
    public String actor() { return actor; }
    /** @return causal correlation */
    public CorrelationId correlationId() { return correlationId; }
    /** @return immutable recording timestamp */
    public Instant recordedAt() { return recordedAt; }
}
