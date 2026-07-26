package io.zartra.bedwars.replay.api;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Immutable, deterministic event-stream entry (ZBW-REPLAY-001, ZBW-REPLAY-003). */
public final class ReplayEvent {
    /** Existing authoritative fact source. */
    public enum Source { GAME, SHOP, PROGRESSION }

    private final String eventId;
    private final long sequence;
    private final long offsetMillis;
    private final Instant occurredAt;
    private final Source source;
    private final String type;
    private final Map<String, String> attributes;

    /** Creates a validated immutable event. */
    public ReplayEvent(final String eventId, final long sequence, final long offsetMillis,
                       final Instant occurredAt, final Source source, final String type,
                       final Map<String, String> attributes) {
        if (sequence < 0 || offsetMillis < 0) {
            throw new IllegalArgumentException("sequence and offset must be non-negative");
        }
        this.eventId = requireText(eventId, "eventId");
        this.sequence = sequence;
        this.offsetMillis = offsetMillis;
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
        this.source = Objects.requireNonNull(source, "source");
        this.type = requireText(type, "type");
        if (this.eventId.length() > 160 || this.type.length() > 160) {
            throw new IllegalArgumentException("eventId and type must not exceed 160 characters");
        }
        final Map<String, String> copy = new LinkedHashMap<String, String>(
                Objects.requireNonNull(attributes, "attributes"));
        if (copy.containsKey(null) || copy.containsValue(null)) {
            throw new IllegalArgumentException("attributes cannot contain null");
        }
        if (copy.size() > 32) { throw new IllegalArgumentException("attributes exceed 32 entries"); }
        for (Map.Entry<String, String> entry : copy.entrySet()) {
            if (entry.getKey().trim().isEmpty() || entry.getKey().length() > 128
                    || entry.getValue().length() > 512) {
                throw new IllegalArgumentException("attribute key or value is malformed");
            }
        }
        this.attributes = Collections.unmodifiableMap(copy);
    }

    private static String requireText(final String value, final String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    /** Returns stable duplicate-suppression identity. */ public String eventId() { return eventId; }
    /** Returns zero-based sequence within one replay. */ public long sequence() { return sequence; }
    /** Returns offset from replay creation in milliseconds. */ public long offsetMillis() { return offsetMillis; }
    /** Returns source occurrence instant. */ public Instant occurredAt() { return occurredAt; }
    /** Returns authoritative source boundary. */ public Source source() { return source; }
    /** Returns version-neutral semantic type. */ public String type() { return type; }
    /** Returns immutable string metadata. */ public Map<String, String> attributes() { return attributes; }
}
