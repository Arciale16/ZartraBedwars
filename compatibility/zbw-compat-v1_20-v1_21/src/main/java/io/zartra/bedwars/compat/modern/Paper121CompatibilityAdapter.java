package io.zartra.bedwars.compat.modern;

import io.zartra.bedwars.api.capability.CapabilitySet;
import io.zartra.bedwars.api.identity.CapabilityId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.ProviderId;
import io.zartra.bedwars.api.provider.Provider;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.api.version.SemanticVersion;
import io.zartra.bedwars.compat.api.CompatibilityAdapter;
import io.zartra.bedwars.compat.api.CompatibilityOutcome;
import io.zartra.bedwars.compat.api.SemanticKey;
import io.zartra.bedwars.compat.api.SemanticMappingRegistry;
import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/** Exact primary adapter for the M06 Paper 1.21.1 build 133 foundation. */
public final class Paper121CompatibilityAdapter implements CompatibilityAdapter {
    /** Locked server fixture digest. */
    public static final String SERVER_SHA256 =
            "39bd8c00b9e18de91dcabd3cc3dcfa5328685a53b7187a2f63280c22e2d287b9";
    private final SemanticMappingRegistry registry;
    private final TimeSource timeSource;
    private volatile LifecycleState lifecycle = LifecycleState.NEW;

    /** Creates the adapter with complete validated primary mappings. */
    public Paper121CompatibilityAdapter(final TimeSource timeSource) {
        this.timeSource = Objects.requireNonNull(timeSource, "timeSource");
        this.registry = new SemanticMappingRegistry(
                PrimarySemanticMappings.all(), PrimarySemanticMappings.required());
    }

    @Override public RuntimeClaim runtimeClaim() {
        return new RuntimeClaim("Paper", "1.21.1", "133", SERVER_SHA256);
    }
    @Override public CompatibilityOutcome resolve(final SemanticKey key) {
        return registry.snapshot().find(Objects.requireNonNull(key, "key"))
                .map(CompatibilityOutcome::supported)
                .orElseGet(() -> CompatibilityOutcome.unsupported(
                        DefinitionId.of("zartra", "compat/unmapped")));
    }
    @Override public SemanticMappingRegistry.Snapshot mappings() { return registry.snapshot(); }
    @Override public Descriptor descriptor() {
        return Descriptor.of(ProviderId.of("zartra", "paper121_compat"),
                SemanticVersion.parse("1.0.0"), CapabilitySet.of(Arrays.asList(
                        CapabilityId.of("zartra", "compat/material"),
                        CapabilityId.of("zartra", "compat/item"),
                        CapabilityId.of("zartra", "compat/metadata"),
                        CapabilityId.of("zartra", "compat/sound"),
                        CapabilityId.of("zartra", "compat/particle"),
                        CapabilityId.of("zartra", "compat/text"),
                        CapabilityId.of("zartra", "compat/entity"),
                        CapabilityId.of("zartra", "compat/packet"),
                        CapabilityId.of("zartra", "compat/ui"),
                        CapabilityId.of("zartra", "compat/scheduler"))));
    }
    @Override public Provider.Health health() {
        final HealthStatus status = lifecycle == LifecycleState.RUNNING
                ? HealthStatus.HEALTHY : HealthStatus.DISABLED;
        return Provider.Health.of(status, timeSource.now(),
                status == HealthStatus.HEALTHY ? "paper121.compat.ready" : "paper121.compat.stopped");
    }
    @Override public synchronized CompletionStage<Result<LifecycleState>> start() {
        lifecycle = LifecycleState.RUNNING;
        return CompletableFuture.completedFuture(Result.success(lifecycle));
    }
    @Override public synchronized CompletionStage<Result<LifecycleState>> drain(final Duration deadline) {
        positive(deadline);
        lifecycle = LifecycleState.DRAINING;
        return CompletableFuture.completedFuture(Result.success(lifecycle));
    }
    @Override public synchronized CompletionStage<Result<LifecycleState>> stop() {
        lifecycle = LifecycleState.STOPPED;
        return CompletableFuture.completedFuture(Result.success(lifecycle));
    }
    private static void positive(final Duration duration) {
        if (Objects.requireNonNull(duration, "deadline").isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("deadline must be positive");
        }
    }
}
