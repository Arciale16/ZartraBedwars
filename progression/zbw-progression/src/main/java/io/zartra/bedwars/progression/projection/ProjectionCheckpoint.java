package io.zartra.bedwars.progression.projection;

import io.zartra.bedwars.api.identity.EventId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import java.time.Instant;
import java.util.Objects;

/** Immutable checkpoint committed with an inbox projection transaction. */
public final class ProjectionCheckpoint {
    private final EventId eventId;
    private final IdempotencyKey idempotencyKey;
    private final long sequence;
    private final Instant processedAt;

    /** Creates a checkpoint for a non-negative stream sequence. */
    public ProjectionCheckpoint(final EventId eventId, final IdempotencyKey idempotencyKey,
                                final long sequence, final Instant processedAt) {
        if (sequence < 0) { throw new IllegalArgumentException("sequence must be non-negative"); }
        this.eventId = Objects.requireNonNull(eventId, "eventId");
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        this.sequence = sequence;
        this.processedAt = Objects.requireNonNull(processedAt, "processedAt");
    }
    /** @return source event identity */ public EventId eventId() { return eventId; }
    /** @return duplicate-suppression key */ public IdempotencyKey idempotencyKey() { return idempotencyKey; }
    /** @return source stream sequence */ public long sequence() { return sequence; }
    /** @return durable processing time */ public Instant processedAt() { return processedAt; }
}
