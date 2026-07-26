package io.zartra.bedwars.replay.playback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** ZBW-REPLAY-003/004 immutable playback model tests. */
final class PlaybackModelTest {
    @Test
    void snapshotDefensivelyCopiesAndOrdersValues() {
        final Map<String, String> values = new LinkedHashMap<String, String>();
        values.put("z", "last");
        values.put("a", "first");
        final ReplaySnapshot snapshot = new ReplaySnapshot(PlaybackCursor.start(), values);
        values.put("later", "mutation");

        assertEquals("[a, z]", snapshot.values().keySet().toString());
        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.values().put("x", "y"));
    }

    @Test
    void cursorAndPositionHaveValueSemantics() {
        final TimelinePosition first = new TimelinePosition(2, 250L);
        final TimelinePosition same = new TimelinePosition(2, 250L);
        final PlaybackCursor cursor = new PlaybackCursor(first);

        assertEquals(first, same);
        assertEquals(first.hashCode(), same.hashCode());
        assertEquals(3, cursor.nextEventIndex());
        assertEquals(cursor, new PlaybackCursor(same));
        assertNotEquals(first, TimelinePosition.start());
    }

    @Test
    void validatesPositionAndSpeedBounds() {
        assertEquals(PlaybackSpeed.NORMAL, PlaybackSpeed.of(1.0D));
        assertEquals("0.1x", PlaybackSpeed.of(0.10D).toString());
        assertTrue(PlaybackSpeed.of(4.0D).multiplier() == 4.0D);
        assertThrows(IllegalArgumentException.class, () -> PlaybackSpeed.of(0.09D));
        assertThrows(IllegalArgumentException.class, () -> PlaybackSpeed.of(4.01D));
        assertThrows(IllegalArgumentException.class, () -> PlaybackSpeed.of(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> PlaybackSpeed.of(Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> new TimelinePosition(-2, 0L));
        assertThrows(IllegalArgumentException.class, () -> new TimelinePosition(-1, 1L));
    }
}
