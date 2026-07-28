package io.zartra.bedwars.redis;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.redis.api.InvalidationVersion;
import io.zartra.bedwars.redis.api.OperationId;
import io.zartra.bedwars.redis.api.RedisNamespace;
import io.zartra.bedwars.redis.api.SchemaVersion;
import io.zartra.bedwars.redis.coordination.CoordinationEvent;
import io.zartra.bedwars.redis.coordination.VersionedCoordinationBridge;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * Deterministic in-process coordination benchmark for SHARED_40 and PROXY_4 equivalent loads.
 *
 * <p>It measures adapter-side validation/deduplication only; the runbook requires transport
 * latency to be measured separately against the same p95/p99 limits on deployment hardware.
 */
final class RedisCoordinationPerformanceTest {
    private static final RedisNamespace NAMESPACE = RedisNamespace.of(
            "installation", "benchmark", "coordination", SchemaVersion.of(1, 0));

    @Test
    void shared40CoordinationMetadataMeetsLatencyAndMemoryBudgets() {
        benchmark("shared-40", 40, 200);
    }

    @Test
    void proxy4EquivalentCoordinationMetadataMeetsLatencyAndMemoryBudgets() {
        benchmark("proxy-4", 4, 2000);
    }

    private void benchmark(final String profile, final int subjects, final int iterations) {
        final int operations = subjects * iterations;
        VersionedCoordinationBridge bridge = new VersionedCoordinationBridge(
                NAMESPACE, new RedisDeduplicationStore(
                Clock.fixed(Instant.EPOCH, ZoneOffset.UTC), operations + 32),
                subjects + 32);
        long[] nanos = new long[operations];
        int cursor = 0;
        for (int revision = 1; revision <= iterations; revision++) {
            for (int subject = 0; subject < subjects; subject++) {
                CoordinationEvent event = new CoordinationEvent(
                        CoordinationEvent.Type.STATISTICS_VERSION,
                        profile + ':' + subject, InvalidationVersion.of(revision),
                        OperationId.random(), Instant.EPOCH);
                long started = System.nanoTime();
                bridge.accept(event);
                nanos[cursor++] = System.nanoTime() - started;
            }
        }
        Arrays.sort(nanos);
        long p95 = nanos[(int) Math.ceil(nanos.length * 0.95D) - 1];
        long p99 = nanos[(int) Math.ceil(nanos.length * 0.99D) - 1];
        assertTrue(p95 <= 5_000_000L, profile + " p95 exceeded 5 ms: " + p95);
        assertTrue(p99 <= 15_000_000L, profile + " p99 exceeded 15 ms: " + p99);
        assertTrue(bridge.size() <= subjects + 32, profile + " metadata exceeded bound");
    }
}
