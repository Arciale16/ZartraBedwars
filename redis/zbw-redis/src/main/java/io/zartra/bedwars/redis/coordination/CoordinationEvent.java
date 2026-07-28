package io.zartra.bedwars.redis.coordination;

import io.zartra.bedwars.redis.api.InvalidationVersion;
import io.zartra.bedwars.redis.api.OperationId;
import java.time.Instant;
import java.util.Objects;

/** Immutable privacy-safe notification that carries no authoritative domain state. */
public final class CoordinationEvent {
    /** Supported infrastructure-only notification families. */
    public enum Type {
        STATISTICS_INVALIDATION,
        LEADERBOARD_INVALIDATION,
        STATISTICS_VERSION,
        REVIEWER_AVAILABILITY,
        ATLAS_QUEUE_HEALTH,
        ITEM_ROTATION_INVALIDATION,
        PLAYER_PRESENCE,
        ARENA_AVAILABILITY,
        QUEUE_NOTIFICATION,
        REPLAY_METADATA,
        ANNOUNCEMENT
    }

    private final Type type;
    private final String subject;
    private final InvalidationVersion version;
    private final OperationId operationId;
    private final Instant occurredAt;

    /**
     * Creates an event containing only an opaque subject reference.
     *
     * @param type notification family
     * @param subject opaque bounded subject reference
     * @param version monotonic source-owned version
     * @param operationId transport idempotency identity
     * @param occurredAt source timestamp
     */
    public CoordinationEvent(final Type type, final String subject,
                             final InvalidationVersion version,
                             final OperationId operationId, final Instant occurredAt) {
        this.type = Objects.requireNonNull(type, "type");
        this.subject = requireSubject(subject);
        this.version = Objects.requireNonNull(version, "version");
        this.operationId = Objects.requireNonNull(operationId, "operationId");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
    }

    /** @return notification family */ public Type type() { return type; }
    /** @return opaque subject */ public String subject() { return subject; }
    /** @return source-owned version */ public InvalidationVersion version() { return version; }
    /** @return operation identity */ public OperationId operationId() { return operationId; }
    /** @return event timestamp */ public Instant occurredAt() { return occurredAt; }

    private static String requireSubject(final String value) {
        final String checked = Objects.requireNonNull(value, "subject").trim();
        if (checked.isEmpty() || checked.length() > 256) {
            throw new IllegalArgumentException("coordination subject must contain 1..256 characters");
        }
        return checked;
    }
}
