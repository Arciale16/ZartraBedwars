package io.zartra.bedwars.paper.replay.viewer;

import java.util.Objects;

/** Bounded immutable important-event row for the replay menu. */
public final class ReplayMenuEvent {
    private final int eventIndex;
    private final long timestampMillis;
    private final String type;

    /** Creates one deterministic event row. */
    public ReplayMenuEvent(final int eventIndex, final long timestampMillis,
                           final String type) {
        if (eventIndex < 0 || timestampMillis < 0L || type == null
                || type.trim().isEmpty()) {
            throw new IllegalArgumentException("replay menu event is malformed");
        }
        this.eventIndex = eventIndex;
        this.timestampMillis = timestampMillis;
        this.type = type;
    }

    /** @return inclusive event index */ public int eventIndex() { return eventIndex; }
    /** @return replay-relative timestamp */ public long timestampMillis() {
        return timestampMillis;
    }
    /** @return sanitized semantic event type */ public String type() { return type; }

    @Override public boolean equals(final Object other) {
        if (!(other instanceof ReplayMenuEvent)) { return false; }
        final ReplayMenuEvent value = (ReplayMenuEvent) other;
        return eventIndex == value.eventIndex && timestampMillis == value.timestampMillis
                && type.equals(value.type);
    }

    @Override public int hashCode() { return Objects.hash(eventIndex, timestampMillis, type); }
}
