package io.zartra.bedwars.cloudnet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.integration.discovery.ServiceDiscoveryProvider;
import io.zartra.bedwars.api.provider.OptionalProviderLifecycle;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.proxy.api.BackendRegistration;
import io.zartra.bedwars.proxy.api.CapacitySnapshot;
import io.zartra.bedwars.proxy.api.HealthSnapshot;
import io.zartra.bedwars.redis.api.DegradationMode;
import io.zartra.bedwars.redis.api.FencingToken;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** ZBW-DEPLOY-003/005 scaling, fencing, crash and proxy integration coverage. */
final class CloudNetCoordinatorTest {
    private static final Instant NOW = Instant.parse("2026-07-29T10:00:00Z");
    private static final DefinitionId TEMPLATE = DefinitionId.of("zartra", "arena/default");

    @Test
    void appliesHysteresisCooldownAndBounds() {
        final CloudNetScalingPolicy policy =
                new CloudNetScalingPolicy(1, 3, 8, 80, 20, 2, 1, Duration.ofSeconds(30));
        final CloudNetScalingPolicy.State initial = new CloudNetScalingPolicy.State(
                CloudNetScalingPolicy.Direction.NONE, 0, NOW.minusSeconds(60));
        final List<CloudNetServiceMetadata> loaded = Collections.singletonList(
                metadata("full", ServiceDiscoveryProvider.ServiceState.ONLINE, 15, 1));

        final CloudNetScalingPolicy.Evaluation first = policy.evaluate(loaded, initial, NOW);
        assertEquals(CloudNetScalingPolicy.Direction.UP, first.direction());
        assertEquals(0, first.actions());
        final CloudNetScalingPolicy.Evaluation second =
                policy.evaluate(loaded, first.nextState(), NOW.plusSeconds(1));
        assertEquals(1, second.actions());
        final CloudNetScalingPolicy.Evaluation cooled =
                policy.evaluate(loaded, second.nextState(), NOW.plusSeconds(2));
        assertEquals(0, cooled.actions());
        assertThrows(IllegalArgumentException.class, () ->
                new CloudNetScalingPolicy(2, 1, 0, 80, 20, 1, 1, Duration.ZERO));
    }

    @Test
    void reconcilesWithLeaseAndPublishesWithoutRoutingOwnership() {
        final FakeGateway gateway = new FakeGateway(Collections.singletonList(
                metadata("full", ServiceDiscoveryProvider.ServiceState.ONLINE, 15, 1)));
        final FakeRedis redis = new FakeRedis();
        final FakeProxy proxy = new FakeProxy();
        try (CloudNetServiceAdapter adapter = adapter(gateway)) {
            adapter.start().toCompletableFuture().join();
            final CloudNetServiceCoordinator coordinator =
                    new CloudNetServiceCoordinator(adapter, redis, proxy, clock());
            final CloudNetScalingPolicy policy =
                    new CloudNetScalingPolicy(1, 3, 8, 80, 20, 1, 1, Duration.ZERO);
            final Result<CloudNetServiceCoordinator.Reconciliation> result =
                    coordinator.reconcile(policy, state(), ServiceDiscoveryProvider.ServiceKind.ARENA,
                            TEMPLATE, 16, NOW.plusSeconds(10)).toCompletableFuture().join();
            assertTrue(result.isSuccess());
            assertEquals(1, result.requireValue().actions());
            assertFalse(result.requireValue().degraded());
            assertEquals(1, gateway.starts.get());
            assertEquals(1, proxy.published.size());
            assertEquals(1, redis.acquires.get());
        }
    }

    @Test
    void pausesUnsafeScalingWhenRedisIsUnavailable() {
        final FakeGateway gateway = new FakeGateway(Collections.emptyList());
        final FakeRedis redis = new FakeRedis();
        redis.mode = DegradationMode.CROSS_NODE_PAUSED;
        try (CloudNetServiceAdapter adapter = adapter(gateway)) {
            adapter.start().toCompletableFuture().join();
            final CloudNetServiceCoordinator coordinator = new CloudNetServiceCoordinator(
                    adapter, redis, new FakeProxy(), clock());
            final CloudNetScalingPolicy policy =
                    new CloudNetScalingPolicy(1, 2, 1, 80, 20, 1, 1, Duration.ZERO);
            final Result<CloudNetServiceCoordinator.Reconciliation> result =
                    coordinator.reconcile(policy, state(), ServiceDiscoveryProvider.ServiceKind.ARENA,
                            TEMPLATE, 16, NOW.plusSeconds(10)).toCompletableFuture().join();
            assertTrue(result.isFailure());
            assertEquals(0, gateway.starts.get());
            assertEquals(0, redis.acquires.get());
        }
    }

    @Test
    void rejectsDuplicateCrashAndReplacesFreshCrashOnce() {
        final FakeGateway gateway = new FakeGateway(Collections.emptyList());
        final FakeRedis redis = new FakeRedis();
        final FakeProxy proxy = new FakeProxy();
        try (CloudNetServiceAdapter adapter = adapter(gateway)) {
            adapter.start().toCompletableFuture().join();
            final CloudNetServiceCoordinator coordinator =
                    new CloudNetServiceCoordinator(adapter, redis, proxy, clock());
            final CloudNetServiceMetadata crashed =
                    metadata("crashed", ServiceDiscoveryProvider.ServiceState.OFFLINE, 0, 2);
            assertTrue(coordinator.replaceCrashed(crashed, NOW.plusSeconds(10))
                    .toCompletableFuture().join().isSuccess());
            assertTrue(coordinator.replaceCrashed(crashed, NOW.plusSeconds(10))
                    .toCompletableFuture().join().isFailure());
            assertEquals(1, gateway.starts.get());
            assertEquals(1, proxy.removed.size());
        }
    }

    @Test
    void rejectsRepeatedFencingToken() {
        final FakeGateway gateway = new FakeGateway(Collections.emptyList());
        final FakeRedis redis = new FakeRedis();
        redis.repeatToken = true;
        try (CloudNetServiceAdapter adapter = adapter(gateway)) {
            adapter.start().toCompletableFuture().join();
            final CloudNetServiceCoordinator coordinator = new CloudNetServiceCoordinator(
                    adapter, redis, new FakeProxy(), clock());
            final CloudNetScalingPolicy policy =
                    new CloudNetScalingPolicy(1, 2, 1, 80, 20, 1, 1, Duration.ZERO);
            assertTrue(coordinator.reconcile(policy, state(),
                    ServiceDiscoveryProvider.ServiceKind.ARENA, TEMPLATE, 16,
                    NOW.plusSeconds(10)).toCompletableFuture().join().isSuccess());
            assertTrue(coordinator.reconcile(policy, state(),
                    ServiceDiscoveryProvider.ServiceKind.ARENA, TEMPLATE, 16,
                    NOW.plusSeconds(10)).toCompletableFuture().join().isFailure());
            assertEquals(1, gateway.starts.get());
        }
    }
    private static CloudNetScalingPolicy.State state() {
        return new CloudNetScalingPolicy.State(
                CloudNetScalingPolicy.Direction.NONE, 0, NOW.minusSeconds(60));
    }

    private static TimeSource clock() { return TimeSource.FixedTimeSource.at(NOW); }

    private static CloudNetServiceAdapter adapter(final FakeGateway gateway) {
        return new CloudNetServiceAdapter(gateway, OptionalProviderLifecycle.Probe.AVAILABLE,
                clock(), new BoundedCloudExecutor(1, 16));
    }

    private static CloudNetServiceMetadata metadata(
            final String id,
            final ServiceDiscoveryProvider.ServiceState state,
            final int occupancy,
            final long revision) {
        return new CloudNetServiceMetadata(
                DefinitionId.of("zartra", "service/" + id), id,
                ServiceDiscoveryProvider.ServiceKind.ARENA, state, TEMPLATE,
                16, occupancy, 1, revision, NOW);
    }

    private static final class FakeGateway implements CloudNetGateway {
        private final List<CloudNetServiceMetadata> services;
        private final AtomicInteger starts = new AtomicInteger();
        private FakeGateway(final List<CloudNetServiceMetadata> services) {
            this.services = services;
        }
        @Override public CompletionStage<List<CloudNetServiceMetadata>> discover() {
            return CompletableFuture.completedFuture(services);
        }
        @Override public CompletionStage<CloudNetServiceMetadata> start(
                final ServiceDiscoveryProvider.ServiceRequest request) {
            final int sequence = starts.incrementAndGet();
            return CompletableFuture.completedFuture(metadata(
                    "replacement-" + sequence,
                    ServiceDiscoveryProvider.ServiceState.STARTING, 0, 1));
        }
        @Override public CompletionStage<Boolean> drain(
                final DefinitionId serviceId, final Instant deadline) {
            return CompletableFuture.completedFuture(Boolean.TRUE);
        }
        @Override public CompletionStage<Boolean> stop(
                final DefinitionId serviceId, final Instant deadline) {
            return CompletableFuture.completedFuture(Boolean.TRUE);
        }
    }

    private static final class FakeRedis implements CloudNetCoordinationPort.Redis {
        private DegradationMode mode = DegradationMode.NORMAL;
        private final AtomicInteger acquires = new AtomicInteger();
        private boolean repeatToken;
        @Override public DegradationMode degradationMode() { return mode; }
        @Override public CompletionStage<Result<FencingToken>> acquire(
                final DefinitionId operationId, final Instant deadline) {
            return CompletableFuture.completedFuture(
                    Result.success(FencingToken.of(repeatToken ? 1 : acquires.incrementAndGet())));
        }
    }

    private static final class FakeProxy implements CloudNetCoordinationPort.Proxy {
        private final List<BackendRegistration> published =
                new ArrayList<BackendRegistration>();
        private final List<BackendRegistration> removed =
                new ArrayList<BackendRegistration>();
        @Override public void publish(
                final BackendRegistration registration,
                final CapacitySnapshot capacity,
                final HealthSnapshot health) {
            published.add(registration);
        }
        @Override public void remove(final BackendRegistration registration) {
            removed.add(registration);
        }
    }
}
