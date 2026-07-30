package io.zartra.bedwars.compat.api;

import io.zartra.bedwars.api.capability.CapabilitySet;
import io.zartra.bedwars.api.identity.CapabilityId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.ProviderId;
import io.zartra.bedwars.api.provider.Provider;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.api.version.SemanticVersion;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Platform-neutral lifecycle implementation for one exact, locked server fixture.
 *
 * <p>Version modules supply only safe symbolic platform mappings. No Bukkit, Paper, NMS or packet
 * implementation class crosses this boundary.</p>
 */
public final class VersionedCompatibilityAdapter implements CompatibilityAdapter {
    private static final DefinitionId UNMAPPED =
            DefinitionId.of("zartra", "compat/unmapped");
    private final RuntimeClaim claim;
    private final Descriptor descriptor;
    private final SemanticMappingRegistry registry;
    private final Set<SemanticKey> fallbacks;
    private final Set<SemanticKey> degraded;
    private final TimeSource timeSource;
    private volatile LifecycleState lifecycle = LifecycleState.NEW;

    /** Creates a validated adapter for one exact runtime. */
    public VersionedCompatibilityAdapter(
            final RuntimeClaim claim,
            final ProviderId providerId,
            final Collection<CompatibilityMapping> mappings,
            final Set<SemanticKey> fallbacks,
            final Set<SemanticKey> degraded,
            final TimeSource timeSource) {
        this.claim = Objects.requireNonNull(claim, "claim");
        this.timeSource = Objects.requireNonNull(timeSource, "timeSource");
        final List<CompatibilityMapping> copy =
                new ArrayList<CompatibilityMapping>(Objects.requireNonNull(mappings, "mappings"));
        final Set<SemanticKey> required = new LinkedHashSet<SemanticKey>();
        final EnumSet<SemanticKey.Kind> kinds = EnumSet.noneOf(SemanticKey.Kind.class);
        for (CompatibilityMapping mapping : copy) {
            required.add(Objects.requireNonNull(mapping, "mapping").semanticKey());
            kinds.add(mapping.semanticKey().kind());
        }
        this.fallbacks = immutableSubset(fallbacks, required, "fallbacks");
        this.degraded = immutableSubset(degraded, required, "degraded");
        if (!Collections.disjoint(this.fallbacks, this.degraded)) {
            throw new IllegalArgumentException("fallback and degraded keys must be disjoint");
        }
        registry = new SemanticMappingRegistry(copy, required);
        final List<CapabilityId> capabilities = new ArrayList<CapabilityId>();
        for (SemanticKey.Kind kind : kinds) {
            capabilities.add(CapabilityId.of("zartra",
                    "compat/" + kind.name().toLowerCase(java.util.Locale.ROOT)));
        }
        descriptor = Descriptor.of(Objects.requireNonNull(providerId, "providerId"),
                SemanticVersion.parse("1.0.0"), CapabilitySet.of(capabilities));
    }

    private static Set<SemanticKey> immutableSubset(
            final Set<SemanticKey> candidate,
            final Set<SemanticKey> required,
            final String label) {
        final Set<SemanticKey> copy =
                new LinkedHashSet<SemanticKey>(Objects.requireNonNull(candidate, label));
        if (copy.contains(null) || !required.containsAll(copy)) {
            throw new IllegalArgumentException(label + " must contain mapped keys only");
        }
        return Collections.unmodifiableSet(copy);
    }

    @Override public RuntimeClaim runtimeClaim() { return claim; }

    @Override public CompatibilityOutcome resolve(final SemanticKey key) {
        final CompatibilityMapping mapping = registry.snapshot()
                .find(Objects.requireNonNull(key, "key")).orElse(null);
        if (mapping == null) {
            return CompatibilityOutcome.unsupported(UNMAPPED);
        }
        if (fallbacks.contains(key)) {
            return CompatibilityOutcome.fallback(mapping,
                    DefinitionId.of("zartra", "compat/fallback/" + key.kind().name()
                            .toLowerCase(java.util.Locale.ROOT)));
        }
        if (degraded.contains(key)) {
            return CompatibilityOutcome.degraded(mapping,
                    DefinitionId.of("zartra", "compat/degraded/" + key.kind().name()
                            .toLowerCase(java.util.Locale.ROOT)), true);
        }
        return CompatibilityOutcome.supported(mapping);
    }

    @Override public SemanticMappingRegistry.Snapshot mappings() { return registry.snapshot(); }
    @Override public Descriptor descriptor() { return descriptor; }

    @Override public Provider.Health health() {
        final HealthStatus status = lifecycle == LifecycleState.RUNNING
                ? HealthStatus.HEALTHY : HealthStatus.DISABLED;
        return Provider.Health.of(status, timeSource.now(),
                status == HealthStatus.HEALTHY ? "compat.ready" : "compat.stopped");
    }

    @Override public synchronized CompletionStage<Result<LifecycleState>> start() {
        lifecycle = LifecycleState.RUNNING;
        return CompletableFuture.completedFuture(Result.success(lifecycle));
    }

    @Override public synchronized CompletionStage<Result<LifecycleState>> drain(
            final Duration deadline) {
        if (Objects.requireNonNull(deadline, "deadline").isZero() || deadline.isNegative()) {
            throw new IllegalArgumentException("deadline must be positive");
        }
        lifecycle = LifecycleState.DRAINING;
        return CompletableFuture.completedFuture(Result.success(lifecycle));
    }

    @Override public synchronized CompletionStage<Result<LifecycleState>> stop() {
        lifecycle = LifecycleState.STOPPED;
        return CompletableFuture.completedFuture(Result.success(lifecycle));
    }
}
