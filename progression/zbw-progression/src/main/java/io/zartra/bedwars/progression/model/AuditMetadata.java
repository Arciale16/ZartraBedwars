package io.zartra.bedwars.progression.model;

import io.zartra.bedwars.api.identity.CorrelationId;
import java.time.Instant;
import java.util.Objects;

/** Immutable authorship and time metadata for an auditable progression mutation. */
public final class AuditMetadata {
    private final String actor;
    private final CorrelationId correlationId;
    private final Instant createdAt;
    private final Instant updatedAt;

    /** Creates validated audit metadata. */
    public AuditMetadata(final String actor, final CorrelationId correlationId,
                         final Instant createdAt, final Instant updatedAt) {
        if (actor == null || actor.trim().isEmpty() || actor.length() > 128) {
            throw new IllegalArgumentException("actor must contain 1..128 characters");
        }
        this.actor = actor;
        this.correlationId = Objects.requireNonNull(correlationId, "correlationId");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("updatedAt must not precede createdAt");
        }
    }

    /** @return audit actor */ public String actor() { return actor; }
    /** @return causal correlation */ public CorrelationId correlationId() { return correlationId; }
    /** @return creation time */ public Instant createdAt() { return createdAt; }
    /** @return last mutation time */ public Instant updatedAt() { return updatedAt; }
}
