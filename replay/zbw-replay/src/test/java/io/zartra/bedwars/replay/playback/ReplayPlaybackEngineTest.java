package io.zartra.bedwars.replay.playback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.replay.api.ReplayEvent;
import io.zartra.bedwars.replay.api.ReplayId;
import io.zartra.bedwars.replay.api.ReplayMetadata;
import io.zartra.bedwars.replay.api.ReplaySession;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** ZBW-REPLAY-003/004/005/009 deterministic playback regression tests. */
final class ReplayPlaybackEngineTest {
    private final ReplayPlaybackEngine engine = new ReplayPlaybackEngine();

    @Test
    void appliesEventsInOrderAndCompletesWithoutMutatingPriorState() {
        final PlaybackSession ready = engine.load(recording());
        final PlaybackSession playing = engine.play(ready);
        final PlaybackSession first = engine.advance(playing);
        final PlaybackSession completed = engine.advance(first);

        assertEquals(PlaybackState.READY, ready.state());
        assertEquals(-1, ready.cursor().position().eventIndex());
        assertEquals(PlaybackState.PLAYING, first.state());
        assertEquals("red", first.snapshot().values().get("team"));
        assertEquals(PlaybackState.COMPLETED, completed.state());
        assertEquals(1, completed.cursor().position().eventIndex());
        assertEquals("blue", completed.snapshot().values().get("team"));
        assertNotSame(first.snapshot(), completed.snapshot());
    }

    @Test
    void pausesResumesAndRetainsConfiguredSpeed() {
        final PlaybackSession playing = engine.play(engine.load(recording()));
        final PlaybackSession faster = engine.changeSpeed(playing, PlaybackSpeed.of(2.0D));
        final PlaybackSession paused = engine.pause(faster);
        final PlaybackSession resumed = engine.play(paused);

        assertEquals(PlaybackState.PAUSED, paused.state());
        assertEquals(PlaybackState.PLAYING, resumed.state());
        assertEquals(PlaybackSpeed.of(2.0D), resumed.speed());
        assertEquals(paused.cursor(), resumed.cursor());
    }

    @Test
    void seeksByIndexAndTimestampUsingInclusiveDeterministicPositions() {
        final PlaybackSession ready = engine.load(recording());
        final PlaybackSession atFirst = engine.seekToEvent(ready, 0);
        final PlaybackSession beforeFirst = engine.seekToTimestamp(atFirst, 9L);
        final PlaybackSession atSecond = engine.seekToTimestamp(beforeFirst, 20L);

        assertEquals(0, atFirst.cursor().position().eventIndex());
        assertEquals("red", atFirst.snapshot().values().get("team"));
        assertEquals(-1, beforeFirst.cursor().position().eventIndex());
        assertTrue(beforeFirst.snapshot().values().isEmpty());
        assertEquals(1, atSecond.cursor().position().eventIndex());
        assertEquals("blue", atSecond.snapshot().values().get("team"));
    }

    @Test
    void restoresOnlySnapshotThatMatchesDeterministicRebuild() {
        final PlaybackSession ready = engine.load(recording());
        final ReplaySnapshot snapshot = engine.seekToEvent(ready, 0).snapshot();
        final PlaybackSession restored = engine.restoreSnapshot(ready, snapshot);

        assertEquals(PlaybackState.PAUSED, restored.state());
        assertEquals(snapshot, restored.snapshot());
        final Map<String, String> tamperedValues =
                new LinkedHashMap<String, String>(snapshot.values());
        tamperedValues.put("team", "tampered");
        final ReplaySnapshot tampered = new ReplaySnapshot(snapshot.cursor(), tamperedValues);
        assertThrows(IllegalArgumentException.class,
                () -> engine.restoreSnapshot(ready, tampered));
    }

    @Test
    void rebuildIsIdenticalAcrossFreshSessions() {
        final ReplaySnapshot first = engine.seekToEvent(engine.load(recording()), 1).snapshot();
        final ReplaySnapshot second = engine.seekToEvent(engine.load(recording()), 1).snapshot();

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void rejectsInvalidCursorTimestampAndLifecycleState() {
        final PlaybackSession ready = engine.load(recording());

        assertThrows(IllegalArgumentException.class, () -> engine.seekToEvent(ready, 2));
        assertThrows(IllegalArgumentException.class, () -> engine.seekToEvent(ready, -2));
        assertThrows(IllegalArgumentException.class, () -> engine.seekToTimestamp(ready, -1L));
        assertThrows(IllegalStateException.class,
                () -> engine.play(PlaybackSession.create(ready.replayId())));
        assertThrows(IllegalStateException.class, () -> engine.pause(ready));
    }

    @Test
    void convertsMalformedEventApplicationIntoSanitizedFailedState() {
        final ReplayPlaybackEngine broken = new ReplayPlaybackEngine((snapshot, event) -> {
            throw new IllegalArgumentException("private decoder detail");
        });
        final PlaybackSession failed = broken.advance(broken.play(broken.load(recording())));

        assertEquals(PlaybackState.FAILED, failed.state());
        assertEquals("corrupted-event", failed.failureReason().get());
        assertEquals(-1, failed.cursor().position().eventIndex());
    }

    @Test
    void rejectsUnfinishedRecordingAndCompletesEmptyReplaySafely() {
        final ReplaySession unfinished = baseSession().start();
        final PlaybackSession failed = engine.load(unfinished);
        final ReplaySession empty = baseSession().start().complete();
        final PlaybackSession completed = engine.play(engine.load(empty));

        assertEquals(PlaybackState.FAILED, failed.state());
        assertEquals("replay-not-playable", failed.failureReason().get());
        assertEquals(PlaybackState.COMPLETED, completed.state());
    }

    private static ReplaySession recording() {
        return baseSession().start()
                .record(event("event-0", 0L, 10L, "red"))
                .record(event("event-1", 1L, 20L, "blue"))
                .complete();
    }

    private static ReplaySession baseSession() {
        final ReplayMetadata metadata = new ReplayMetadata(
                ReplayId.parse("00000000-0000-0000-0000-000000000017"),
                MatchId.parse("00000000-0000-0000-0000-000000000008"),
                Instant.parse("2026-07-26T00:00:00Z"), 1,
                Collections.emptySet(), false);
        return ReplaySession.create(metadata);
    }

    private static ReplayEvent event(final String id, final long sequence,
                                     final long offset, final String team) {
        final Map<String, String> attributes = new LinkedHashMap<String, String>();
        attributes.put("team", team);
        return new ReplayEvent(id, sequence, offset,
                Instant.parse("2026-07-26T00:00:" + (10L + sequence) + "Z"),
                ReplayEvent.Source.GAME, "match.transition", attributes);
    }
}
