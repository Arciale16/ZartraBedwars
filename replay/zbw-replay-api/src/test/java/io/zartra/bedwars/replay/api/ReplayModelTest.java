package io.zartra.bedwars.replay.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.api.identity.PlayerId;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** ZBW-REPLAY-001/003 immutable model, ordering and state regression tests. */
class ReplayModelTest {
    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

    @Test void identifiersAndMetadataAreImmutable() {
        final ReplayId id = ReplayId.of(UUID.fromString("00000000-0000-0000-0000-000000000017"));
        final PlayerId player = PlayerId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        final Set<PlayerId> participants = new HashSet<PlayerId>();
        participants.add(player);
        final ReplayMetadata metadata = new ReplayMetadata(id, MatchId.random(), START, 1,
                participants, false);
        participants.clear();
        assertEquals(id, ReplayId.parse(id.toString()));
        assertEquals(0, id.compareTo(ReplayId.parse(id.toString())));
        assertEquals(id.hashCode(), ReplayId.parse(id.toString()).hashCode());
        assertEquals(1, metadata.participants().size());
        assertThrows(UnsupportedOperationException.class, () -> metadata.participants().clear());
        assertThrows(IllegalArgumentException.class, () -> new ReplayMetadata(
                id, MatchId.random(), START, 0, Collections.<PlayerId>emptySet(), false));
    }

    @Test void eventAndTimelineDefensivelyCopyAndOrder() {
        final Map<String, String> values = new HashMap<String, String>();
        values.put("actor", "one");
        final ReplayEvent first = event("one", 0, 1, values);
        values.put("actor", "changed");
        assertEquals("one", first.attributes().get("actor"));
        assertThrows(UnsupportedOperationException.class, () -> first.attributes().clear());
        final ReplayTimeline one = ReplayTimeline.empty().append(first);
        assertNotSame(ReplayTimeline.empty(), one);
        assertEquals(one, one.append(first));
        assertTrue(one.contains("one"));
        assertFalse(one.contains("missing"));
        assertEquals(1, one.nextSequence());
        assertThrows(IllegalArgumentException.class, () -> one.append(event("gap", 2, 2,
                Collections.<String, String>emptyMap())));
        assertThrows(IllegalArgumentException.class, () -> one.append(event("back", 1, 0,
                Collections.<String, String>emptyMap())));
        assertThrows(IllegalArgumentException.class, () -> event("bad", -1, 0,
                Collections.<String, String>emptyMap()));
        assertThrows(IllegalArgumentException.class, () -> new ReplayEvent(" ", 0, 0, START,
                ReplayEvent.Source.GAME, "type", Collections.<String, String>emptyMap()));
    }

    @Test void sessionRejectsInvalidStatesAndPreservesEvents() {
        final ReplaySession created = ReplaySession.create(metadata(false));
        assertEquals(ReplayState.CREATED, created.state());
        assertThrows(IllegalStateException.class, created::complete);
        assertThrows(IllegalStateException.class, () -> created.record(event("one", 0, 0,
                Collections.<String, String>emptyMap())));
        final ReplaySession recording = created.start().record(event("one", 0, 0,
                Collections.<String, String>emptyMap()));
        final ReplaySession completed = recording.complete();
        assertEquals(1, completed.timeline().events().size());
        assertEquals(ReplayState.ARCHIVED, completed.archive().state());
        assertThrows(IllegalStateException.class, completed::start);
        final ReplaySession failed = created.fail("capture unavailable");
        assertEquals(ReplayState.FAILED, failed.state());
        assertEquals("capture unavailable", failed.failureReason().get());
        assertThrows(IllegalStateException.class, () -> completed.fail("late"));
        assertThrows(IllegalArgumentException.class, () -> created.fail(" "));
    }

    private static ReplayEvent event(final String id, final long sequence, final long offset,
                                     final Map<String, String> attributes) {
        return new ReplayEvent(id, sequence, offset, START.plusMillis(offset), ReplayEvent.Source.GAME,
                "game.fact", attributes);
    }

    static ReplayMetadata metadata(final boolean protectedEvidence) {
        final Set<PlayerId> participants = Collections.singleton(PlayerId.of(
                UUID.fromString("00000000-0000-0000-0000-000000000001")));
        return new ReplayMetadata(ReplayId.random(), MatchId.random(), START, 1,
                participants, protectedEvidence);
    }
}
