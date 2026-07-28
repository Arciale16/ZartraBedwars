package io.zartra.bedwars.redis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.redis.api.DegradationMode;
import io.zartra.bedwars.redis.api.RedisAvailability;
import io.zartra.bedwars.redis.api.RedisHealth;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

final class RedisFailureRecoveryTest {
    private static final Instant START = Instant.parse("2026-07-28T00:00:00Z");

    @Test
    void flushRestartAndCompleteCacheLossRebuildWithinRto() {
        MutableClock clock = new MutableClock(START);
        RedisRecoveryTracker tracker = new RedisRecoveryTracker(clock);

        tracker.unavailable();
        assertEquals(RedisRecoveryTracker.State.DEGRADED, tracker.state());
        clock.advance(Duration.ofMinutes(2));
        tracker.rebuilding();
        clock.advance(Duration.ofMinutes(2));
        tracker.recovered();

        assertEquals(RedisRecoveryTracker.State.RECOVERED, tracker.state());
        assertEquals(Duration.ofMinutes(4), tracker.elapsed());
        assertTrue(tracker.metRto());

        tracker.unavailable();
        clock.advance(Duration.ofMinutes(6));
        tracker.rebuilding();
        tracker.recovered();
        assertFalse(tracker.metRto());
    }

    @Test
    void invalidRecoveryTransitionsAndRepeatedPartitionsFailClosed() {
        MutableClock clock = new MutableClock(START);
        RedisRecoveryTracker tracker = new RedisRecoveryTracker(clock);

        assertThrows(IllegalStateException.class, tracker::rebuilding);
        assertThrows(IllegalStateException.class, tracker::recovered);
        tracker.unavailable();
        clock.advance(Duration.ofSeconds(10));
        tracker.unavailable();
        assertEquals(Duration.ofSeconds(10), tracker.elapsed());
    }

    @Test
    void diagnosticsAreSanitizedAndBounded() {
        RedisHealth health = RedisHealth.of(RedisAvailability.DEGRADED,
                DegradationMode.CROSS_NODE_PAUSED, "timeout", 12, START);
        RedisDiagnostics diagnostics = new RedisDiagnostics(
                health, RedisCircuitBreaker.State.OPEN, 200, 20);

        assertEquals(RedisAvailability.DEGRADED, diagnostics.availability());
        assertEquals(DegradationMode.CROSS_NODE_PAUSED, diagnostics.degradationMode());
        assertEquals(RedisCircuitBreaker.State.OPEN, diagnostics.circuitState());
        assertEquals("timeout", diagnostics.diagnosticCode());
        assertEquals(12, diagnostics.pendingOperations());
        assertEquals(200, diagnostics.deduplicationEntries());
        assertEquals(20, diagnostics.coordinationMetadataEntries());
        assertEquals(START, diagnostics.observedAt());
        assertThrows(IllegalArgumentException.class, () -> new RedisDiagnostics(
                health, RedisCircuitBreaker.State.OPEN,
                RedisDeduplicationStore.MAX_ENTRIES + 1, 0));
    }

    private static final class MutableClock extends Clock {
        private Instant value;

        private MutableClock(final Instant value) { this.value = value; }
        private void advance(final Duration duration) { value = value.plus(duration); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(final ZoneId zone) { return this; }
        @Override public Instant instant() { return value; }
    }
}
