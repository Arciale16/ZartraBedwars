package io.zartra.bedwars.api.event;

import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.EventId;
import io.zartra.bedwars.api.identity.EventTypeId;
import java.time.Instant;
import java.util.Objects;

/** Immutable metadata shared by every public event. */
public final class EventMetadata {
    private final EventId eventId;
    private final EventTypeId eventType;
    private final CorrelationId correlationId;
    private final Instant occurredAt;
    private final long sequence;
    private final int schemaVersion;
    private final ThreadContext threadContext;

    private EventMetadata(final EventId eventId, final EventTypeId eventType,
                          final CorrelationId correlationId, final Instant occurredAt,
                          final long sequence, final int schemaVersion,
                          final ThreadContext threadContext) {
        if (sequence < 0) { throw new IllegalArgumentException("sequence must be non-negative"); }
        if (schemaVersion < 1) { throw new IllegalArgumentException("schemaVersion must be positive"); }
        this.eventId = Objects.requireNonNull(eventId, "eventId");
        this.eventType = Objects.requireNonNull(eventType, "eventType");
        this.correlationId = Objects.requireNonNull(correlationId, "correlationId");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
        this.sequence = sequence;
        this.schemaVersion = schemaVersion;
        this.threadContext = Objects.requireNonNull(threadContext, "threadContext");
    }

    /** @return immutable event metadata */
    public static EventMetadata of(final EventId eventId, final EventTypeId eventType,
                                   final CorrelationId correlationId, final Instant occurredAt,
                                   final long sequence, final int schemaVersion,
                                   final ThreadContext threadContext) {
        return new EventMetadata(eventId, eventType, correlationId, occurredAt, sequence,
                schemaVersion, threadContext);
    }

    /** @return unique event identity */ public EventId eventId() { return eventId; }
    /** @return versioned event type */ public EventTypeId eventType() { return eventType; }
    /** @return causal correlation identity */ public CorrelationId correlationId() { return correlationId; }
    /** @return occurrence instant, captured before publication */ public Instant occurredAt() { return occurredAt; }
    /** @return non-negative order within the event's causal stream */ public long sequence() { return sequence; }
    /** @return positive payload schema version */ public int schemaVersion() { return schemaVersion; }
    /** @return callback threading guarantee */ public ThreadContext threadContext() { return threadContext; }

    @Override public int hashCode() {
        return Objects.hash(eventId, eventType, correlationId, occurredAt, sequence, schemaVersion, threadContext);
    }
    @Override public boolean equals(final Object other) {
        if (this == other) { return true; }
        if (!(other instanceof EventMetadata)) { return false; }
        final EventMetadata that = (EventMetadata) other;
        return sequence == that.sequence && schemaVersion == that.schemaVersion
                && eventId.equals(that.eventId) && eventType.equals(that.eventType)
                && correlationId.equals(that.correlationId) && occurredAt.equals(that.occurredAt)
                && threadContext == that.threadContext;
    }

    /** Thread context in which a listener is invoked. */
    public enum ThreadContext {
        /** Minecraft/platform owner thread; callback must be non-blocking and bounded. */
        OWNER_THREAD,
        /** Bounded application worker; no platform object may be assumed thread-safe. */
        APPLICATION_WORKER,
        /** Provider-controlled asynchronous callback; no gameplay mutation is permitted. */
        PROVIDER_WORKER
    }
}
