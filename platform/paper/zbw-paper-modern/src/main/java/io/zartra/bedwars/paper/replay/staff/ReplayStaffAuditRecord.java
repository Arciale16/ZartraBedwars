package io.zartra.bedwars.paper.replay.staff;

import io.zartra.bedwars.replay.api.ReplayId;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Immutable deterministic record for one completed staff replay action. */
public final class ReplayStaffAuditRecord {
    private final long sequence;
    private final Instant occurredAt;
    private final UUID actorId;
    private final ReplayStaffAction action;
    private final ReplayId replayId;
    private final ReplayStaffResult.Status outcome;

    /** Creates a fully classified audit record. */
    public ReplayStaffAuditRecord(final long sequence, final Instant occurredAt,
                                  final UUID actorId, final ReplayStaffAction action,
                                  final ReplayId replayId,
                                  final ReplayStaffResult.Status outcome) {
        if (sequence < 0L) { throw new IllegalArgumentException("sequence must be non-negative"); }
        this.sequence = sequence;
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
        this.actorId = Objects.requireNonNull(actorId, "actorId");
        this.action = Objects.requireNonNull(action, "action");
        this.replayId = replayId;
        this.outcome = Objects.requireNonNull(outcome, "outcome");
    }

    /** @return monotonic service-local sequence */ public long sequence() { return sequence; }
    /** @return injected audit timestamp */ public Instant occurredAt() { return occurredAt; }
    /** @return staff actor identity */ public UUID actorId() { return actorId; }
    /** @return stable action */ public ReplayStaffAction action() { return action; }
    /** @return target replay when applicable */ public Optional<ReplayId> replayId() {
        return Optional.ofNullable(replayId);
    }
    /** @return sanitized result */ public ReplayStaffResult.Status outcome() { return outcome; }
}
