package io.zartra.bedwars.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.event.ApiEvent;
import io.zartra.bedwars.api.event.EventMetadata;
import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.EventId;
import io.zartra.bedwars.api.identity.EventTypeId;
import io.zartra.bedwars.api.result.ApiError;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.api.time.MonotonicTimeSource;
import io.zartra.bedwars.api.time.TimeSource;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ResultTimeEventTest {
    @Test
    void resultCarriesExactlyOneTypedBranch() {
        final Result<Integer> success = Result.success(4);
        assertTrue(success.isSuccess());
        assertEquals(8, success.map(value -> value * 2).requireValue());
        assertFalse(success.error().isPresent());
        final ApiError error = ApiError.of(DefinitionId.of("test", "failure"), "test.failure",
                ApiError.RetryDisposition.RETRYABLE);
        final Result<Integer> failure = Result.failure(error);
        assertTrue(failure.isFailure());
        assertEquals(error, failure.error().get());
        assertEquals(error, assertThrows(Result.ResultAccessException.class, failure::requireValue).error());
        assertThrows(NullPointerException.class, () -> Result.success(null));
        assertThrows(IllegalArgumentException.class, () -> ApiError.of(
                DefinitionId.of("test", "bad"), "Contains spaces", ApiError.RetryDisposition.PERMANENT));
    }

    @Test
    void clocksExposeDeterministicAndMonotonicBoundaries() {
        final Instant fixed = Instant.parse("2026-07-14T12:00:00Z");
        assertEquals(fixed, TimeSource.FixedTimeSource.at(fixed).now());
        assertTrue(!TimeSource.SystemTimeSource.INSTANCE.now().isBefore(Instant.parse("2020-01-01T00:00:00Z")));
        final long first = MonotonicTimeSource.SystemMonotonicTimeSource.INSTANCE.readNanos();
        final long second = MonotonicTimeSource.SystemMonotonicTimeSource.INSTANCE.readNanos();
        assertTrue(second >= first);
    }

    @Test
    void eventMetadataPreservesOrderingThreadAndCancellationSemantics() {
        final EventMetadata metadata = EventMetadata.of(EventId.of(new UUID(1L, 2L)),
                EventTypeId.of("test", "event"), CorrelationId.of(new UUID(3L, 4L)),
                Instant.parse("2026-07-14T12:00:00Z"), 7L, 2,
                EventMetadata.ThreadContext.APPLICATION_WORKER);
        assertEquals(7L, metadata.sequence());
        assertEquals(2, metadata.schemaVersion());
        assertEquals(EventMetadata.ThreadContext.APPLICATION_WORKER, metadata.threadContext());
        assertEquals(metadata, EventMetadata.of(metadata.eventId(), metadata.eventType(),
                metadata.correlationId(), metadata.occurredAt(), 7L, 2, metadata.threadContext()));
        assertThrows(IllegalArgumentException.class, () -> EventMetadata.of(metadata.eventId(),
                metadata.eventType(), metadata.correlationId(), metadata.occurredAt(), -1L, 1,
                EventMetadata.ThreadContext.OWNER_THREAD));
        final ApiEvent.Decision proceed = ApiEvent.Decision.proceed();
        assertFalse(proceed.isCancellation());
        assertEquals(null, proceed.cancellationReasonOrNull());
        final DefinitionId reason = DefinitionId.of("test", "cancelled");
        assertEquals(reason, ApiEvent.Decision.cancel(reason).cancellationReasonOrNull());
    }
}
