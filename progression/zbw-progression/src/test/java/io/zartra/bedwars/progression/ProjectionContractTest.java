package io.zartra.bedwars.progression;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.zartra.bedwars.api.event.EventMetadata;
import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.EventId;
import io.zartra.bedwars.api.identity.EventTypeId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.progression.model.PlayerProgressionId;
import io.zartra.bedwars.progression.projection.ProgressionEventInput;
import io.zartra.bedwars.progression.projection.ProjectionCheckpoint;
import io.zartra.bedwars.progression.projection.ProjectionRecoveryState;
import io.zartra.bedwars.progression.projection.ProjectionResult;
import io.zartra.bedwars.storage.api.RecordRevision;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Tests projection serialization and idempotency boundaries. */
class ProjectionContractTest {
    private static final Instant NOW = Instant.parse("2026-07-19T12:00:00Z");

    private ProgressionEventInput input(final byte[] payload) {
        final EventMetadata metadata = EventMetadata.of(EventId.of(new UUID(1, 1)),
                EventTypeId.of("zartra", "match/completed"), CorrelationId.of(new UUID(2, 2)),
                NOW, 3, 1, EventMetadata.ThreadContext.APPLICATION_WORKER);
        return new ProgressionEventInput(metadata,
                PlayerProgressionId.of(PlayerId.of(new UUID(3, 3))),
                DefinitionId.of("zartra", "progression/match-completed"),
                IdempotencyKey.of("zartra", "event/1"), payload);
    }

    @Test void eventInputDefensivelyCopiesBoundedPayload() {
        final byte[] payload = new byte[] {1, 2, 3};
        final ProgressionEventInput input = input(payload);
        payload[0] = 9;
        assertArrayEquals(new byte[] {1, 2, 3}, input.payload());
        final byte[] copy = input.payload();
        copy[1] = 9;
        assertArrayEquals(new byte[] {1, 2, 3}, input.payload());
        assertEquals(input, input(new byte[] {1, 2, 3}));
        assertEquals(input.hashCode(), input(new byte[] {1, 2, 3}).hashCode());
        assertFalse(input.equals("event"));
        assertThrows(IllegalArgumentException.class, () -> input(new byte[1_048_577]));
        assertEquals(3L, input.metadata().sequence());
        assertNotNull(input.playerId());
        assertNotNull(input.eventKind());
        assertNotNull(input.idempotencyKey());
    }

    @Test void checkpointsResultsAndRecoveryRepresentDuplicateHandling() {
        final ProjectionCheckpoint checkpoint = new ProjectionCheckpoint(EventId.of(new UUID(1, 1)),
                IdempotencyKey.of("zartra", "event/1"), 3, NOW);
        assertEquals(3L, checkpoint.sequence());
        assertEquals(NOW, checkpoint.processedAt());
        assertThrows(IllegalArgumentException.class, () -> new ProjectionCheckpoint(checkpoint.eventId(), checkpoint.idempotencyKey(), -1, NOW));
        final ProjectionResult applied = new ProjectionResult(ProjectionResult.Status.APPLIED,
                checkpoint, RecordRevision.of(2), null);
        assertEquals(ProjectionResult.Status.APPLIED, applied.status());
        assertEquals(2L, applied.revision().get().value());
        assertFalse(applied.detail().isPresent());
        final ProjectionResult duplicate = new ProjectionResult(ProjectionResult.Status.DUPLICATE,
                checkpoint, null, null);
        assertFalse(duplicate.revision().isPresent());
        final ProjectionResult failed = new ProjectionResult(ProjectionResult.Status.RETRYABLE_FAILURE,
                null, null, "timeout");
        assertEquals("timeout", failed.detail().get());
        assertThrows(IllegalArgumentException.class, () -> new ProjectionResult(ProjectionResult.Status.APPLIED, checkpoint, null, "bad"));
        assertThrows(IllegalArgumentException.class, () -> new ProjectionResult(ProjectionResult.Status.REJECTED, null, null, repeat('x', 257)));
        final ProjectionRecoveryState recovery = new ProjectionRecoveryState(input(new byte[0]), 2,
                NOW.plusSeconds(5), "timeout");
        assertEquals(2, recovery.attempts());
        assertEquals(NOW.plusSeconds(5), recovery.nextAttemptAt());
        assertNotNull(recovery.input());
        assertEquals("timeout", recovery.lastFailureCode().get());
        assertThrows(IllegalArgumentException.class, () -> new ProjectionRecoveryState(input(new byte[0]), 0, NOW, null));
        assertThrows(IllegalArgumentException.class, () -> new ProjectionRecoveryState(input(new byte[0]), 101, NOW, null));
        assertThrows(IllegalArgumentException.class, () -> new ProjectionRecoveryState(input(new byte[0]), 1, NOW, ""));
    }

    private static String repeat(final char value, final int count) {
        final char[] result = new char[count];
        java.util.Arrays.fill(result, value);
        return new String(result);
    }
}
