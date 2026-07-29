package io.zartra.bedwars.paper.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.capability.CapabilitySet;
import io.zartra.bedwars.api.identity.ProviderId;
import io.zartra.bedwars.api.provider.OptionalProviderLifecycle;
import io.zartra.bedwars.api.provider.Provider;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.api.version.SemanticVersion;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.Test;

/** Optional registration, duplicate and cleanup tests for Paper provider composition. */
final class PaperProviderIntegrationRuntimeTest {
    @Test void emptyRuntimeStartsAndClosesSafely() {
        PaperProviderIntegrationRuntime runtime = new PaperProviderIntegrationRuntime();
        assertTrue(runtime.start().toCompletableFuture().join().isEmpty());
        runtime.close().toCompletableFuture().join();
    }

    @Test void providerAbsenceDoesNotBreakPaperStartup() {
        PaperProviderIntegrationRuntime runtime = new PaperProviderIntegrationRuntime();
        runtime.install(provider("absent", OptionalProviderLifecycle.Probe.ABSENT));
        assertEquals(Provider.LifecycleState.STOPPED,
                runtime.start().toCompletableFuture().join().get(0).requireValue());
        runtime.close().toCompletableFuture().join();
    }

    @Test void duplicateProviderIdentityIsRejected() {
        PaperProviderIntegrationRuntime runtime = new PaperProviderIntegrationRuntime();
        runtime.install(provider("duplicate", OptionalProviderLifecycle.Probe.AVAILABLE));
        assertThrows(IllegalStateException.class, () -> runtime.install(
                provider("duplicate", OptionalProviderLifecycle.Probe.AVAILABLE)));
    }

    private static Provider provider(final String id,
                                     final OptionalProviderLifecycle.Probe probe) {
        OptionalProviderLifecycle lifecycle = new OptionalProviderLifecycle(
                ProviderId.of("test", id), SemanticVersion.parse("1.0.0"),
                CapabilitySet.empty(), TimeSource.FixedTimeSource.at(Instant.EPOCH),
                "provider.test", probe);
        return new Provider() {
            @Override public Descriptor descriptor() { return lifecycle.descriptor(); }
            @Override public Health health() { return lifecycle.health(); }
            @Override public CompletionStage<Result<LifecycleState>> start() {
                return lifecycle.start();
            }
            @Override public CompletionStage<Result<LifecycleState>> drain(
                    final Duration deadline) {
                return lifecycle.drain(deadline);
            }
            @Override public CompletionStage<Result<LifecycleState>> stop() {
                return lifecycle.stop();
            }
        };
    }
}
