package io.zartra.bedwars.replay.playback;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Immutable deterministic reconstructed replay state (ZBW-REPLAY-003/004). */
public final class ReplaySnapshot {
    private final PlaybackCursor cursor;
    private final Map<String, String> values;

    /** Creates a defensive, key-ordered snapshot. */
    public ReplaySnapshot(final PlaybackCursor cursor, final Map<String, String> values) {
        this.cursor = Objects.requireNonNull(cursor, "cursor");
        Objects.requireNonNull(values, "values");
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                throw new IllegalArgumentException("snapshot values cannot contain null");
            }
        }
        final Map<String, String> copy = new TreeMap<String, String>(values);
        this.values = Collections.unmodifiableMap(copy);
    }

    /** Returns an empty snapshot at the start of the timeline. */
    public static ReplaySnapshot empty() {
        return new ReplaySnapshot(PlaybackCursor.start(), Collections.<String, String>emptyMap());
    }

    /** Returns the last-applied event cursor. */
    public PlaybackCursor cursor() {
        return cursor;
    }

    /** Returns immutable reconstructed values in stable key order. */
    public Map<String, String> values() {
        return values;
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof ReplaySnapshot)) {
            return false;
        }
        final ReplaySnapshot snapshot = (ReplaySnapshot) other;
        return cursor.equals(snapshot.cursor) && values.equals(snapshot.values);
    }

    @Override
    public int hashCode() {
        return 31 * cursor.hashCode() + values.hashCode();
    }
}
