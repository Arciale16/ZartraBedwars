package io.zartra.bedwars.api.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.capability.CapabilitySet;
import io.zartra.bedwars.api.identity.ProviderId;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.api.version.SemanticVersion;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Contract tests for optional provider startup and fallback behavior. */
final class OptionalProviderLifecycleTest {
    private static OptionalProviderLifecycle lifecycle(
            final OptionalProviderLifecycle.Probe probe) {
        return new OptionalProviderLifecycle(ProviderId.of("zartra", "test"),
                SemanticVersion.parse("1.0.0"), CapabilitySet.empty(),
                TimeSource.FixedTimeSource.at(Instant.EPOCH), "provider.test", probe);
    }

    @Test void absentProviderStopsWithoutFailingStartup() {
        OptionalProviderLifecycle lifecycle = lifecycle(OptionalProviderLifecycle.Probe.ABSENT);
        assertEquals(Provider.LifecycleState.STOPPED,
                lifecycle.start().toCompletableFuture().join().requireValue());
        assertEquals(Provider.HealthStatus.DISABLED, lifecycle.health().status());
        assertFalse(lifecycle.available());
    }

    @Test void incompatibleProviderFailsClosed() {
        OptionalProviderLifecycle lifecycle =
                lifecycle(OptionalProviderLifecycle.Probe.INCOMPATIBLE);
        assertEquals(Provider.LifecycleState.FAILED,
                lifecycle.start().toCompletableFuture().join().requireValue());
        assertEquals(Provider.HealthStatus.UNAVAILABLE, lifecycle.health().status());
    }

    @Test void availableProviderRunsAndDrains() {
        OptionalProviderLifecycle lifecycle =
                lifecycle(OptionalProviderLifecycle.Probe.AVAILABLE);
        lifecycle.start().toCompletableFuture().join();
        assertTrue(lifecycle.available());
        assertEquals(Provider.LifecycleState.STOPPED,
                lifecycle.drain(Duration.ofSeconds(1))
                        .toCompletableFuture().join().requireValue());
        assertFalse(lifecycle.available());
    }
}
