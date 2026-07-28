package io.zartra.bedwars.redis.coordination;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.redis.RedisDeduplicationStore;
import io.zartra.bedwars.redis.api.DegradationMode;
import io.zartra.bedwars.redis.api.FencingToken;
import io.zartra.bedwars.redis.api.InvalidationVersion;
import io.zartra.bedwars.redis.api.OperationId;
import io.zartra.bedwars.redis.api.RedisAvailability;
import io.zartra.bedwars.redis.api.RedisHealth;
import io.zartra.bedwars.redis.api.RedisKey;
import io.zartra.bedwars.redis.api.RedisNamespace;
import io.zartra.bedwars.redis.api.SchemaVersion;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.Test;

final class CoordinationBridgeTest {
    private static final Instant NOW = Instant.parse("2026-07-28T00:00:00Z");
    private static final RedisNamespace NAMESPACE = RedisNamespace.of(
            "installation", "test", "coordination", SchemaVersion.of(1, 0));

    @Test
    void invalidationOrderingDuplicatesAndCacheRebuildRemainDeterministic() {
        VersionedCoordinationBridge bridge = bridge(2);
        CoordinationEvent versionTwo = event(
                CoordinationEvent.Type.STATISTICS_INVALIDATION, "player:opaque", 2);

        assertEquals(VersionedCoordinationBridge.Result.APPLIED, bridge.accept(versionTwo));
        assertEquals(VersionedCoordinationBridge.Result.DUPLICATE, bridge.accept(versionTwo));
        assertEquals(VersionedCoordinationBridge.Result.STALE, bridge.accept(event(
                CoordinationEvent.Type.STATISTICS_INVALIDATION, "player:opaque", 1)));
        assertFalse(bridge.requiresRebuild(CoordinationEvent.Type.STATISTICS_INVALIDATION,
                "player:opaque", InvalidationVersion.of(2)));
        assertTrue(bridge.requiresRebuild(CoordinationEvent.Type.STATISTICS_INVALIDATION,
                "player:opaque", InvalidationVersion.of(3)));
        assertTrue(bridge.requiresRebuild(CoordinationEvent.Type.LEADERBOARD_INVALIDATION,
                "wins:global", InvalidationVersion.of(1)));
    }

    @Test
    void metadataIsBoundedAndItemRotationUsesOnlyVersionNotifications() {
        VersionedCoordinationBridge bridge = bridge(2);
        assertEquals(VersionedCoordinationBridge.Result.APPLIED, bridge.accept(event(
                CoordinationEvent.Type.ITEM_ROTATION_INVALIDATION, "test:weekly", 7)));
        assertEquals(VersionedCoordinationBridge.Result.APPLIED, bridge.accept(event(
                CoordinationEvent.Type.LEADERBOARD_INVALIDATION, "wins:global", 2)));
        assertEquals(VersionedCoordinationBridge.Result.APPLIED, bridge.accept(event(
                CoordinationEvent.Type.REPLAY_METADATA, "replay:opaque", 1)));
        assertEquals(2, bridge.size());
        assertTrue(bridge.requiresRebuild(CoordinationEvent.Type.ITEM_ROTATION_INVALIDATION,
                "test:weekly", InvalidationVersion.of(7)));
    }

    @Test
    void codecRoundTripsAllSafeNotificationDataAndRejectsMalformedSchemas() {
        CoordinationEventCodec codec = new CoordinationEventCodec();
        CoordinationEvent original = event(
                CoordinationEvent.Type.ARENA_AVAILABILITY, "arena:opaque|safe", 4);
        CoordinationEvent decoded = codec.decode(codec.encode(original));

        assertEquals(original.type(), decoded.type());
        assertEquals(original.subject(), decoded.subject());
        assertEquals(original.version(), decoded.version());
        assertEquals(original.operationId(), decoded.operationId());
        assertEquals(original.occurredAt(), decoded.occurredAt());
        assertThrows(IllegalArgumentException.class,
                () -> codec.decode("2|bad".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        assertThrows(IllegalArgumentException.class, () -> codec.decode(new byte[0]));
        assertThrows(IllegalArgumentException.class, () -> new CoordinationEvent(
                CoordinationEvent.Type.ANNOUNCEMENT, "", InvalidationVersion.of(1),
                OperationId.random(), NOW));
    }

    @Test
    void atlasReservationRejectsStaleFencesAndRaces() {
        MemoryLeasePort leases = new MemoryLeasePort(available());
        AtlasReservationCoordinator coordinator = new AtlasReservationCoordinator(leases, 2);
        RedisKey caseKey = RedisKey.of(NAMESPACE, "atlas-reservation", "case-1");

        AtlasReservationCoordinator.Result first = coordinator.reserve(
                caseKey, "reviewer-a", Duration.ofSeconds(30)).toCompletableFuture().join();
        assertEquals(AtlasReservationCoordinator.Status.ACQUIRED, first.status());
        assertEquals(FencingToken.of(1), first.token());

        leases.next = FencingToken.of(1);
        assertEquals(AtlasReservationCoordinator.Status.CONFLICT, coordinator.reserve(
                caseKey, "reviewer-b", Duration.ofSeconds(30)).toCompletableFuture().join().status());
        assertFalse(coordinator.renew(caseKey, "reviewer-a", FencingToken.of(2),
                Duration.ofSeconds(30)).toCompletableFuture().join());
        assertTrue(coordinator.renew(caseKey, "reviewer-a", FencingToken.of(1),
                Duration.ofSeconds(30)).toCompletableFuture().join());
    }

    @Test
    void atlasStopsCrossNodeReservationsWhenRedisIsUnavailable() {
        MemoryLeasePort leases = new MemoryLeasePort(RedisHealth.of(
                RedisAvailability.UNAVAILABLE, DegradationMode.CROSS_NODE_PAUSED,
                "offline", 0, NOW));
        AtlasReservationCoordinator coordinator = new AtlasReservationCoordinator(leases, 1);

        AtlasReservationCoordinator.Result result = coordinator.reserve(
                RedisKey.of(NAMESPACE, "atlas-reservation", "case-2"),
                "reviewer-a", Duration.ofSeconds(30)).toCompletableFuture().join();
        assertEquals(AtlasReservationCoordinator.Status.CROSS_NODE_PAUSED, result.status());
        assertEquals(null, result.token());
        assertEquals(0, leases.acquireCalls);

        leases.failHealth = true;
        assertEquals(AtlasReservationCoordinator.Status.CROSS_NODE_PAUSED, coordinator.reserve(
                RedisKey.of(NAMESPACE, "atlas-reservation", "case-3"),
                "reviewer-a", Duration.ofSeconds(30)).toCompletableFuture().join().status());
    }

    @Test
    void invalidConfigurationAndReservationBoundsFailClosed() {
        assertThrows(IllegalArgumentException.class, () -> bridge(0));
        assertThrows(IllegalArgumentException.class,
                () -> new AtlasReservationCoordinator(new MemoryLeasePort(available()), 0));
        AtlasReservationCoordinator coordinator =
                new AtlasReservationCoordinator(new MemoryLeasePort(available()), 1);
        RedisKey key = RedisKey.of(NAMESPACE, "atlas-reservation", "case");
        assertThrows(IllegalArgumentException.class,
                () -> coordinator.reserve(key, "", Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class,
                () -> coordinator.reserve(key, "reviewer", Duration.ofMinutes(16)));
    }

    private VersionedCoordinationBridge bridge(final int capacity) {
        return new VersionedCoordinationBridge(NAMESPACE,
                new RedisDeduplicationStore(Clock.fixed(NOW, ZoneOffset.UTC), 32), capacity);
    }

    private CoordinationEvent event(final CoordinationEvent.Type type,
                                    final String subject, final long version) {
        return new CoordinationEvent(type, subject, InvalidationVersion.of(version),
                OperationId.random(), NOW);
    }

    private RedisHealth available() {
        return RedisHealth.of(RedisAvailability.AVAILABLE, DegradationMode.NORMAL,
                "ok", 0, NOW);
    }

    private static final class MemoryLeasePort
            implements AtlasReservationCoordinator.LeasePort {
        private RedisHealth health;
        private FencingToken next = FencingToken.of(1);
        private int acquireCalls;
        private boolean failHealth;

        private MemoryLeasePort(final RedisHealth health) {
            this.health = health;
        }

        @Override
        public CompletionStage<FencingToken> acquire(
                final RedisKey key, final String holder, final long ttlMillis) {
            acquireCalls++;
            return CompletableFuture.completedFuture(next);
        }

        @Override
        public CompletionStage<Boolean> renew(final RedisKey key, final String holder,
                                              final FencingToken token, final long ttlMillis) {
            return CompletableFuture.completedFuture(Boolean.TRUE);
        }

        @Override
        public CompletionStage<RedisHealth> health() {
            if (failHealth) {
                CompletableFuture<RedisHealth> failed = new CompletableFuture<RedisHealth>();
                failed.completeExceptionally(new IllegalStateException("offline"));
                return failed;
            }
            return CompletableFuture.completedFuture(health);
        }
    }
}
