package io.zartra.bedwars.progression.objective;

import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.progression.model.AuditMetadata;
import io.zartra.bedwars.progression.model.PlayerProgressionId;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Durable immutable execution state for one versioned objective and player. */
public final class ObjectiveRuntimeState {
    /** Terminal and active states. */
    public enum Status { /** Accepting events. */ ACTIVE, /** Target reached. */ COMPLETED, /** Deadline elapsed. */ EXPIRED }

    private final ObjectiveId objectiveId;
    private final PlayerProgressionId playerId;
    private final int definitionVersion;
    private final long value;
    private final long completionCount;
    private final Status status;
    private final long revision;
    private final Optional<IdempotencyKey> lastEvent;
    private final Optional<Instant> expiresAt;
    private final AuditMetadata audit;

    /** Creates a validated state snapshot. */
    public ObjectiveRuntimeState(final ObjectiveId objectiveId, final PlayerProgressionId playerId,
                                 final int definitionVersion, final long value,
                                 final long completionCount, final Status status,
                                 final long revision, final Optional<IdempotencyKey> lastEvent,
                                 final Optional<Instant> expiresAt, final AuditMetadata audit) {
        this.objectiveId = Objects.requireNonNull(objectiveId, "objectiveId");
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        if (definitionVersion < 1 || value < 0 || completionCount < 0 || revision < 0) {
            throw new IllegalArgumentException("version must be positive and counters non-negative");
        }
        this.definitionVersion = definitionVersion;
        this.value = value;
        this.completionCount = completionCount;
        this.status = Objects.requireNonNull(status, "status");
        this.revision = revision;
        this.lastEvent = Objects.requireNonNull(lastEvent, "lastEvent");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    /** Creates an active zero-progress state. */
    public static ObjectiveRuntimeState active(final ObjectiveDefinition definition,
                                               final PlayerProgressionId playerId,
                                               final Optional<Instant> expiresAt,
                                               final AuditMetadata audit) {
        return new ObjectiveRuntimeState(definition.id(), playerId, definition.version(), 0, 0,
                Status.ACTIVE, 0, Optional.empty(), expiresAt, audit);
    }

    /** @return objective identity */ public ObjectiveId objectiveId() { return objectiveId; }
    /** @return player identity */ public PlayerProgressionId playerId() { return playerId; }
    /** @return snapshotted definition version */ public int definitionVersion() { return definitionVersion; }
    /** @return current cycle progress */ public long value() { return value; }
    /** @return completed cycle count */ public long completionCount() { return completionCount; }
    /** @return lifecycle status */ public Status status() { return status; }
    /** @return optimistic revision */ public long revision() { return revision; }
    /** @return last applied event */ public Optional<IdempotencyKey> lastEvent() { return lastEvent; }
    /** @return optional deadline */ public Optional<Instant> expiresAt() { return expiresAt; }
    /** @return audit metadata */ public AuditMetadata audit() { return audit; }
}
