package io.zartra.bedwars.replay.playback;

import java.util.Objects;

/** Immutable cursor over an ordered replay timeline (ZBW-REPLAY-003/004). */
public final class PlaybackCursor {
    private final TimelinePosition position;

    /** Creates a cursor at a validated timeline position. */
    public PlaybackCursor(final TimelinePosition position) {
        this.position = Objects.requireNonNull(position, "position");
    }

    /** Returns a cursor before the first event. */
    public static PlaybackCursor start() {
        return new PlaybackCursor(TimelinePosition.start());
    }

    /** Returns the current timeline position. */
    public TimelinePosition position() {
        return position;
    }

    /** Returns the next event index to apply. */
    public int nextEventIndex() {
        return position.eventIndex() + 1;
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof PlaybackCursor
                && position.equals(((PlaybackCursor) other).position);
    }

    @Override
    public int hashCode() {
        return position.hashCode();
    }
}
