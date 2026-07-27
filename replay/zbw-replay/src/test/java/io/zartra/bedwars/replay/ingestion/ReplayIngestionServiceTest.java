package io.zartra.bedwars.replay.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.event.EventMetadata;
import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.EventId;
import io.zartra.bedwars.api.identity.EventTypeId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.progression.model.PlayerProgressionId;
import io.zartra.bedwars.progression.projection.ProgressionEventInput;
import io.zartra.bedwars.replay.api.ReplayEvent;
import io.zartra.bedwars.replay.api.ReplayId;
import io.zartra.bedwars.replay.api.ReplayMetadata;
import io.zartra.bedwars.replay.api.ReplaySession;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** ZBW-REPLAY-001/003/005 ingestion validation and deterministic timeline tests. */
class ReplayIngestionServiceTest {
    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

    @Test void acceptedEventsAreOrderedAndDuplicateProtected() {
        final ReplayIngestionService service = new ReplayIngestionService();
        final ReplaySession recording = ReplaySession.create(metadata()).start();
        final ProgressionEventInput first = input("00000000-0000-0000-0000-000000000011", 1);
        final ProgressionEventInput second = input("00000000-0000-0000-0000-000000000012", 2);
        final ReplayIngestionResult one = service.ingest(recording, first, "projection-1");
        final ReplayIngestionResult duplicate = service.ingest(one.session(), first, "projection-1");
        final ReplayIngestionResult two = service.ingest(duplicate.session(), second, "projection-2");
        assertEquals(ReplayIngestionResult.Status.ACCEPTED, one.status());
        assertEquals(ReplayIngestionResult.Status.DUPLICATE, duplicate.status());
        assertSame(one.session(), duplicate.session());
        assertEquals(ReplayIngestionResult.Status.ACCEPTED, two.status());
        assertTrue(two.accepted());
        assertFalse(duplicate.accepted());
        assertEquals(0, two.session().timeline().events().get(0).sequence());
        assertEquals(1, two.session().timeline().events().get(1).sequence());
    }

    @Test void malformedUnsupportedAndInvalidStateAreExplicitAndNonMutating() {
        final ReplayIngestionService service = new ReplayIngestionService();
        final ReplaySession recording = ReplaySession.create(metadata()).start();
        final ReplayIngestionResult malformed = service.ingest(recording, input(
                "00000000-0000-0000-0000-000000000013", 1), " ");
        final ReplayIngestionResult temporal = service.ingest(recording,
                inputAt("00000000-0000-0000-0000-000000000014", START.minusMillis(1)), "old");
        final ReplayIngestionResult unsupported = service.ingest(recording, new Object(), "other");
        final ReplayIngestionResult invalidState = service.ingest(
                ReplaySession.create(metadata()), input(
                        "00000000-0000-0000-0000-000000000015", 1), "created");
        assertEquals(ReplayIngestionResult.Status.MALFORMED, malformed.status());
        assertEquals(ReplayIngestionResult.Status.MALFORMED, temporal.status());
        assertEquals(ReplayIngestionResult.Status.UNSUPPORTED, unsupported.status());
        assertEquals(ReplayIngestionResult.Status.INVALID_STATE, invalidState.status());
        assertSame(recording, malformed.session());
        assertSame(recording, temporal.session());
        assertSame(recording, unsupported.session());
        assertEquals("unsupported-source-type", unsupported.detail());
    }

    @Test void identicalInputsProduceIdenticalTimelines() {
        final ReplayIngestionService service = new ReplayIngestionService();
        ReplaySession left = ReplaySession.create(metadata()).start();
        ReplaySession right = ReplaySession.create(left.metadata()).start();
        for (int index = 1; index <= 3; index++) {
            final ProgressionEventInput event = input(String.format(
                    "00000000-0000-0000-0000-%012d", 20 + index), index);
            left = service.ingest(left, event, "source-" + index).session();
            right = service.ingest(right, event, "source-" + index).session();
        }
        for (int index = 0; index < 3; index++) {
            final ReplayEvent leftEvent = left.timeline().events().get(index);
            final ReplayEvent rightEvent = right.timeline().events().get(index);
            assertEquals(leftEvent.eventId(), rightEvent.eventId());
            assertEquals(leftEvent.sequence(), rightEvent.sequence());
            assertEquals(leftEvent.offsetMillis(), rightEvent.offsetMillis());
            assertEquals(leftEvent.type(), rightEvent.type());
            assertEquals(leftEvent.attributes(), rightEvent.attributes());
        }
    }

    private static ReplayMetadata metadata() {
        return new ReplayMetadata(ReplayId.random(), MatchId.random(), START, 1,
                Collections.<PlayerId>emptySet(), false);
    }

    private static ProgressionEventInput input(final String id, final long offset) {
        return inputAt(id, START.plusMillis(offset));
    }

    private static ProgressionEventInput inputAt(final String id, final Instant occurredAt) {
        final PlayerId player = PlayerId.of(
                UUID.fromString("00000000-0000-0000-0000-000000000001"));
        final EventMetadata metadata = EventMetadata.of(EventId.parse(id),
                EventTypeId.of("progression", "reward"), CorrelationId.of(
                        UUID.fromString("00000000-0000-0000-0000-000000000003")),
                occurredAt, 1, 1, EventMetadata.ThreadContext.APPLICATION_WORKER);
        return new ProgressionEventInput(metadata, PlayerProgressionId.of(player),
                DefinitionId.of("progression", "reward"), IdempotencyKey.of("replay", "event"),
                new byte[] {1});
    }
}
