package io.zartra.bedwars.api.provider;

import io.zartra.bedwars.api.capability.CapabilitySet;
import io.zartra.bedwars.api.identity.ProviderId;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.api.version.SemanticVersion;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

/** Thread-safe lifecycle helper for optional, externally supplied provider adapters. */
public final class OptionalProviderLifecycle {
    private final Provider.Descriptor descriptor;
    private final TimeSource timeSource;
    private final String diagnosticPrefix;
    private final Probe probe;
    private final AtomicReference<Provider.LifecycleState> state =
            new AtomicReference<Provider.LifecycleState>(Provider.LifecycleState.NEW);

    /**
     * Creates an optional provider lifecycle.
     *
     * @param providerId stable adapter identity
     * @param version adapter version
     * @param capabilities immutable declared capabilities
     * @param timeSource health observation clock
     * @param diagnosticPrefix safe diagnostic namespace
     * @param probe vendor presence/compatibility result
     */
    public OptionalProviderLifecycle(final ProviderId providerId,
                                     final SemanticVersion version,
                                     final CapabilitySet capabilities,
                                     final TimeSource timeSource,
                                     final String diagnosticPrefix,
                                     final Probe probe) {
        descriptor = Provider.Descriptor.of(providerId, version, capabilities);
        this.timeSource = Objects.requireNonNull(timeSource, "timeSource");
        if (diagnosticPrefix == null
                || !diagnosticPrefix.matches("[a-z0-9][a-z0-9_.-]{0,95}")) {
            throw new IllegalArgumentException("diagnosticPrefix must be safe");
        }
        this.diagnosticPrefix = diagnosticPrefix;
        this.probe = Objects.requireNonNull(probe, "probe");
    }

    /** @return immutable adapter descriptor */
    public Provider.Descriptor descriptor() { return descriptor; }

    /** @return sanitized current provider health */
    public Provider.Health health() {
        final Provider.LifecycleState current = state.get();
        if (probe == Probe.ABSENT) {
            return Provider.Health.of(Provider.HealthStatus.DISABLED, timeSource.now(),
                    diagnosticPrefix + ".absent");
        }
        if (probe == Probe.INCOMPATIBLE || current == Provider.LifecycleState.FAILED) {
            return Provider.Health.of(Provider.HealthStatus.UNAVAILABLE, timeSource.now(),
                    diagnosticPrefix + ".incompatible");
        }
        final Provider.HealthStatus status = current == Provider.LifecycleState.RUNNING
                ? Provider.HealthStatus.HEALTHY : Provider.HealthStatus.DEGRADED;
        return Provider.Health.of(status, timeSource.now(),
                diagnosticPrefix + "." + current.name().toLowerCase(java.util.Locale.ROOT));
    }

    /** @return safe lifecycle result; absence and incompatibility never throw */
    public CompletionStage<Result<Provider.LifecycleState>> start() {
        final Provider.LifecycleState next;
        if (probe == Probe.ABSENT) {
            next = Provider.LifecycleState.STOPPED;
        } else if (probe == Probe.INCOMPATIBLE) {
            next = Provider.LifecycleState.FAILED;
        } else {
            next = Provider.LifecycleState.RUNNING;
        }
        state.set(next);
        return completed(next);
    }

    /** @param deadline non-negative drain deadline @return stopped lifecycle result */
    public CompletionStage<Result<Provider.LifecycleState>> drain(final Duration deadline) {
        Objects.requireNonNull(deadline, "deadline");
        if (deadline.isNegative()) {
            throw new IllegalArgumentException("deadline must not be negative");
        }
        if (state.get() == Provider.LifecycleState.RUNNING) {
            state.set(Provider.LifecycleState.DRAINING);
        }
        state.set(Provider.LifecycleState.STOPPED);
        return completed(Provider.LifecycleState.STOPPED);
    }

    /** @return stopped lifecycle result */
    public CompletionStage<Result<Provider.LifecycleState>> stop() {
        state.set(Provider.LifecycleState.STOPPED);
        return completed(Provider.LifecycleState.STOPPED);
    }

    /** @return whether vendor calls may currently be delegated */
    public boolean available() {
        return probe == Probe.AVAILABLE
                && state.get() == Provider.LifecycleState.RUNNING;
    }

    /** @return immutable presence/compatibility probe */
    public Probe probe() { return probe; }

    private static CompletionStage<Result<Provider.LifecycleState>> completed(
            final Provider.LifecycleState value) {
        return CompletableFuture.completedFuture(Result.success(value));
    }

    /** Optional vendor discovery result. */
    public enum Probe {
        /** Vendor plugin and compatible API binding are available. */
        AVAILABLE,
        /** Vendor plugin is not installed; native/no-provider fallback remains active. */
        ABSENT,
        /** Vendor was found but its API contract is unsupported. */
        INCOMPATIBLE
    }
}
