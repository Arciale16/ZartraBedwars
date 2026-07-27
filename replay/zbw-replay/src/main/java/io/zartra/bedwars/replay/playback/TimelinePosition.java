package io.zartra.bedwars.replay.playback;

/** Immutable event index and replay-relative timestamp (ZBW-REPLAY-003/005). */
public final class TimelinePosition {
    private final int eventIndex;
    private final long offsetMillis;

    /** Creates a position; index -1 denotes the point before the first event. */
    public TimelinePosition(final int eventIndex, final long offsetMillis) {
        if (eventIndex < -1 || offsetMillis < 0L || (eventIndex == -1 && offsetMillis != 0L)) {
            throw new IllegalArgumentException("invalid timeline position");
        }
        this.eventIndex = eventIndex;
        this.offsetMillis = offsetMillis;
    }

    /** Returns the position before the first event. */
    public static TimelinePosition start() {
        return new TimelinePosition(-1, 0L);
    }

    /** Returns the zero-based last-applied event index, or -1 before playback. */
    public int eventIndex() {
        return eventIndex;
    }

    /** Returns the replay-relative timestamp. */
    public long offsetMillis() {
        return offsetMillis;
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof TimelinePosition)) {
            return false;
        }
        final TimelinePosition position = (TimelinePosition) other;
        return eventIndex == position.eventIndex && offsetMillis == position.offsetMillis;
    }

    @Override
    public int hashCode() {
        return 31 * eventIndex + Long.valueOf(offsetMillis).hashCode();
    }
}
