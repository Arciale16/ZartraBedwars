package io.zartra.bedwars.cloudnet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.integration.discovery.ServiceDiscoveryProvider;
import io.zartra.bedwars.api.provider.OptionalProviderLifecycle;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.api.time.TimeSource;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** ZBW-DEPLOY-005 and ZBW-ADDON-226..233 adapter regression coverage. */
final class CloudNetAdapterTest {
    private static final Instant NOW = Instant.parse("2026-07-29T10:00:00Z");
    private static final DefinitionId TEMPLATE = DefinitionId.of("zartra", "arena/default");

    @Test
    void validatesSecretFreeMetadataAndFreshness() {
        final CloudNetServiceMetadata first = metadata(
                "service-b", 2, 1, ServiceDiscoveryProvider.ServiceState.ONLINE);
        final CloudNetServiceMetadata second = metadata(
                "service-b", 2, 2, ServiceDiscoveryProvider.ServiceState.DRAINING);

        assertTrue(second.supersedes(first));
        assertFalse(first.supersedes(second));
        assertEquals(16, second.snapshot().capacity());
        assertThrows(IllegalArgumentException.class, () -> new CloudNetServiceMetadata(
                DefinitionId.of("zartra", "service/b"), "contains secret",
                ServiceDiscoveryProvider.ServiceKind.ARENA,
                ServiceDiscoveryProvider.ServiceState.ONLINE, TEMPLATE,
                1, 0, 1, 1, NOW));
        assertThrows(IllegalArgumentException.class, () -> new CloudNetServiceMetadata(
                DefinitionId.of("zartra", "service/b"), "service-b",
                ServiceDiscoveryProvider.ServiceKind.ARENA,
                ServiceDiscoveryProvider.ServiceState.ONLINE, TEMPLATE,
                1, 2, 1, 1, NOW));
    }

    @Test
    void discoversDeterministicallyAndDeduplicatesStarts() {
        final FakeGateway gateway = new FakeGateway(Arrays.asList(
                metadata("service-b", 1, 1, ServiceDiscoveryProvider.ServiceState.ONLINE),
                metadata("service-a", 1, 1, ServiceDiscoveryProvider.ServiceState.ONLINE)));
        try (CloudNetServiceAdapter adapter = adapter(gateway,
                OptionalProviderLifecycle.Probe.AVAILABLE)) {
            adapter.start().toCompletableFuture().join();
            final Result<List<ServiceDiscoveryProvider.ServiceSnapshot>> discovered =
                    adapter.discover().toCompletableFuture().join();
            assertTrue(discovered.isSuccess());
            assertEquals("zartra:service/service-a",
                    discovered.requireValue().get(0).serviceId().toString());

            final ServiceDiscoveryProvider.ServiceRequest request = request("start-1");
            final Result<ServiceDiscoveryProvider.ServiceSnapshot> first =
                    adapter.request(request).toCompletableFuture().join();
            final Result<ServiceDiscoveryProvider.ServiceSnapshot> duplicate =
                    adapter.request(request).toCompletableFuture().join();
            assertTrue(first.isSuccess());
            assertEquals(first.requireValue().serviceId(), duplicate.requireValue().serviceId());
            assertEquals(1, gateway.starts.get());
            assertTrue(adapter.drain(first.requireValue().serviceId(), NOW.plusSeconds(5))
                    .toCompletableFuture().join().requireValue());
            assertTrue(adapter.stop(first.requireValue().serviceId(), NOW.plusSeconds(5))
                    .toCompletableFuture().join().requireValue());
        }
    }

    @Test
    void failsClosedForAbsenceDeadlineFailureAndMalformedDiscovery() {
        final FakeGateway gateway = new FakeGateway(Collections.emptyList());
        try (CloudNetServiceAdapter absent = adapter(gateway,
                OptionalProviderLifecycle.Probe.ABSENT)) {
            absent.start().toCompletableFuture().join();
            assertTrue(absent.discover().toCompletableFuture().join().isFailure());
        }
        gateway.services = Arrays.asList(
                metadata("same", 1, 1, ServiceDiscoveryProvider.ServiceState.ONLINE),
                metadata("same", 2, 1, ServiceDiscoveryProvider.ServiceState.ONLINE));
        try (CloudNetServiceAdapter available = adapter(gateway,
                OptionalProviderLifecycle.Probe.AVAILABLE)) {
            available.start().toCompletableFuture().join();
            assertTrue(available.discoverMetadata().toCompletableFuture().join().isFailure());
            assertTrue(available.request(new ServiceDiscoveryProvider.ServiceRequest(
                    IdempotencyKey.of("zartra", "expired"),
                    ServiceDiscoveryProvider.ServiceKind.ARENA, TEMPLATE, 16, NOW))
                    .toCompletableFuture().join().isFailure());
            gateway.fail = true;
            gateway.services = Collections.emptyList();
            assertTrue(available.discover().toCompletableFuture().join().isFailure());
        }
    }

    @Test
    void enforcesBoundedExecutorConfigurationAndLifecycle() {
        assertThrows(IllegalArgumentException.class, () -> new BoundedCloudExecutor(0, 1));
        assertThrows(IllegalArgumentException.class, () -> new BoundedCloudExecutor(1, 2048));
        final BoundedCloudExecutor executor = new BoundedCloudExecutor(1, 1);
        assertEquals(0, executor.queueDepth());
        executor.close();
    }

    private static CloudNetServiceAdapter adapter(
            final FakeGateway gateway, final OptionalProviderLifecycle.Probe probe) {
        return new CloudNetServiceAdapter(gateway, probe,
                TimeSource.FixedTimeSource.at(NOW), new BoundedCloudExecutor(1, 8));
    }

    private static ServiceDiscoveryProvider.ServiceRequest request(final String id) {
        return new ServiceDiscoveryProvider.ServiceRequest(
                IdempotencyKey.of("zartra", id),
                ServiceDiscoveryProvider.ServiceKind.ARENA,
                TEMPLATE, 16, NOW.plusSeconds(10));
    }

    static CloudNetServiceMetadata metadata(
            final String id, final long epoch, final long revision,
            final ServiceDiscoveryProvider.ServiceState state) {
        return new CloudNetServiceMetadata(
                DefinitionId.of("zartra", "service/" + id), id,
                ServiceDiscoveryProvider.ServiceKind.ARENA, state,
                TEMPLATE, 16, 2, epoch, revision, NOW);
    }

    private static final class FakeGateway implements CloudNetGateway {
        private List<CloudNetServiceMetadata> services;
        private final AtomicInteger starts = new AtomicInteger();
        private boolean fail;
        private FakeGateway(final List<CloudNetServiceMetadata> services) {
            this.services = services;
        }
        @Override public CompletionStage<List<CloudNetServiceMetadata>> discover() {
            if (fail) {
                final CompletableFuture<List<CloudNetServiceMetadata>> failed =
                        new CompletableFuture<List<CloudNetServiceMetadata>>();
                failed.completeExceptionally(new IllegalStateException("unavailable"));
                return failed;
            }
            return CompletableFuture.completedFuture(services);
        }
        @Override public CompletionStage<CloudNetServiceMetadata> start(
                final ServiceDiscoveryProvider.ServiceRequest request) {
            final int index = starts.incrementAndGet();
            return CompletableFuture.completedFuture(metadata(
                    "started-" + index, index, 1,
                    ServiceDiscoveryProvider.ServiceState.STARTING));
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
}
