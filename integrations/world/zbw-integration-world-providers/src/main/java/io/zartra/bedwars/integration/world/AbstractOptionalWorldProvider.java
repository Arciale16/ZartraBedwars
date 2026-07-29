package io.zartra.bedwars.integration.world;

import io.zartra.bedwars.api.capability.CapabilitySet;
import io.zartra.bedwars.api.identity.CapabilityId;
import io.zartra.bedwars.api.identity.ProviderId;
import io.zartra.bedwars.api.provider.OptionalProviderLifecycle;
import io.zartra.bedwars.api.provider.Provider;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.api.version.SemanticVersion;
import io.zartra.bedwars.world.api.WorldKey;
import io.zartra.bedwars.world.api.WorldOperation;
import io.zartra.bedwars.world.api.WorldProvider;
import java.time.Duration;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

/** Shared lifecycle/fallback mechanics for isolated optional world-provider adapters. */
abstract class AbstractOptionalWorldProvider implements WorldProvider, Provider {
    private final Descriptor descriptor;
    private final WorldProviderGateway gateway;
    private final OptionalProviderLifecycle.Probe probe;
    private final WorldProvider fallback;
    private final TimeSource timeSource;
    private final String diagnosticPrefix;
    private final AtomicReference<LifecycleState> state =
            new AtomicReference<LifecycleState>(LifecycleState.NEW);

    AbstractOptionalWorldProvider(
            final ProviderId id,
            final SemanticVersion version,
            final Collection<CapabilityId> capabilities,
            final WorldProviderGateway gateway,
            final OptionalProviderLifecycle.Probe probe,
            final WorldProvider fallback,
            final TimeSource timeSource,
            final String diagnosticPrefix) {
        descriptor = Descriptor.of(id, version, CapabilitySet.of(capabilities));
        this.gateway = Objects.requireNonNull(gateway, "gateway");
        this.probe = Objects.requireNonNull(probe, "probe");
        this.fallback = Objects.requireNonNull(fallback, "fallback");
        this.timeSource = Objects.requireNonNull(timeSource, "timeSource");
        if (diagnosticPrefix == null
                || !diagnosticPrefix.matches("[a-z0-9][a-z0-9_.-]{0,95}")) {
            throw new IllegalArgumentException("diagnosticPrefix must be safe");
        }
        this.diagnosticPrefix = diagnosticPrefix;
    }

    @Override public final ProviderId id() { return descriptor.id(); }
    @Override public final Descriptor descriptor() { return descriptor; }

    @Override
    public final Health health() {
        final LifecycleState current = state.get();
        if (probe == OptionalProviderLifecycle.Probe.ABSENT) {
            return Health.of(HealthStatus.DISABLED, timeSource.now(),
                    diagnosticPrefix + ".absent");
        }
        if (probe == OptionalProviderLifecycle.Probe.INCOMPATIBLE
                || current == LifecycleState.FAILED) {
            return Health.of(HealthStatus.UNAVAILABLE, timeSource.now(),
                    diagnosticPrefix + ".incompatible");
        }
        return Health.of(current == LifecycleState.RUNNING
                        ? HealthStatus.HEALTHY : HealthStatus.DEGRADED,
                timeSource.now(),
                diagnosticPrefix + "." + current.name().toLowerCase(
                        java.util.Locale.ROOT));
    }

    @Override
    public final CompletionStage<Result<LifecycleState>> start() {
        if (probe == OptionalProviderLifecycle.Probe.ABSENT) {
            state.set(LifecycleState.STOPPED);
            return completed(LifecycleState.STOPPED);
        }
        if (probe == OptionalProviderLifecycle.Probe.INCOMPATIBLE) {
            state.set(LifecycleState.FAILED);
            return completed(LifecycleState.FAILED);
        }
        if (!state.compareAndSet(LifecycleState.NEW, LifecycleState.STARTING)
                && !state.compareAndSet(LifecycleState.STOPPED,
                        LifecycleState.STARTING)) {
            return completed(state.get());
        }
        try {
            return gateway.compatible().handle((compatible, failure) -> {
                final LifecycleState next = failure == null
                        && Boolean.TRUE.equals(compatible)
                        ? LifecycleState.RUNNING : LifecycleState.FAILED;
                state.set(next);
                return Result.success(next);
            });
        } catch (RuntimeException failure) {
            state.set(LifecycleState.FAILED);
            return completed(LifecycleState.FAILED);
        }
    }

    @Override
    public final CompletionStage<Result<LifecycleState>> drain(
            final Duration deadline) {
        Objects.requireNonNull(deadline, "deadline");
        if (deadline.isNegative()) {
            throw new IllegalArgumentException("deadline must not be negative");
        }
        if (state.get() == LifecycleState.RUNNING) {
            state.set(LifecycleState.DRAINING);
        }
        state.set(LifecycleState.STOPPED);
        return completed(LifecycleState.STOPPED);
    }

    @Override
    public final CompletionStage<Result<LifecycleState>> stop() {
        state.set(LifecycleState.STOPPED);
        return completed(LifecycleState.STOPPED);
    }

    @Override
    public final Plan plan(final WorldOperation operation) {
        Objects.requireNonNull(operation, "operation");
        if (state.get() != LifecycleState.RUNNING) {
            return fallback.plan(operation);
        }
        try {
            final Plan plan = Objects.requireNonNull(
                    gateway.plan(operation), "gateway plan");
            if (!plan.operation().operationId().equals(operation.operationId())
                    || !plan.operation().target().equals(operation.target())
                    || plan.operation().type() != operation.type()) {
                throw new IllegalArgumentException(
                        "gateway plan does not match the requested operation");
            }
            return plan;
        } catch (RuntimeException failure) {
            return fallback.plan(operation);
        }
    }

    @Override
    public final ResourceSnapshot snapshot(final WorldKey world) {
        Objects.requireNonNull(world, "world");
        if (state.get() != LifecycleState.RUNNING) {
            return fallback.snapshot(world);
        }
        try {
            return Objects.requireNonNull(
                    gateway.snapshot(world), "gateway snapshot");
        } catch (RuntimeException failure) {
            return fallback.snapshot(world);
        }
    }

    private static CompletionStage<Result<LifecycleState>> completed(
            final LifecycleState value) {
        return CompletableFuture.completedFuture(Result.success(value));
    }
}
