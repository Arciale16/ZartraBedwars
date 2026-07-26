package io.zartra.bedwars.paper.replay.visual;

import java.util.Objects;

/** Immutable important event retained by the bounded visual projection. */
public final class VisualMatchEvent {
    private final long sequence;
    private final long offsetMillis;
    private final String type;
    private final String subject;

    /** Creates one ordered semantic visual event. */
    public VisualMatchEvent(final long sequence, final long offsetMillis,
                            final String type, final String subject) {
        if (sequence < 0L || offsetMillis < 0L || type == null || type.trim().isEmpty()
                || subject == null || subject.trim().isEmpty()) {
            throw new IllegalArgumentException("visual event is malformed");
        }
        this.sequence = sequence;
        this.offsetMillis = offsetMillis;
        this.type = type;
        this.subject = subject;
    }

    /** @return replay sequence */ public long sequence() { return sequence; }
    /** @return replay-relative time */ public long offsetMillis() { return offsetMillis; }
    /** @return semantic type */ public String type() { return type; }
    /** @return event subject */ public String subject() { return subject; }

    @Override public boolean equals(final Object other) {
        if (!(other instanceof VisualMatchEvent)) { return false; }
        final VisualMatchEvent value = (VisualMatchEvent) other;
        return sequence == value.sequence && offsetMillis == value.offsetMillis
                && type.equals(value.type) && subject.equals(value.subject);
    }

    @Override public int hashCode() { return Objects.hash(sequence, offsetMillis, type, subject); }
}
