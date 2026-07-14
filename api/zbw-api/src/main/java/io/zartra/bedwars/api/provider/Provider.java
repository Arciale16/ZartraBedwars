package io.zartra.bedwars.api.provider;

import io.zartra.bedwars.api.capability.CapabilitySet;
import io.zartra.bedwars.api.identity.ProviderId;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.api.version.SemanticVersion;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

/**
 * Base asynchronous provider lifecycle.
 *
 * <p>Lifecycle callbacks may run on a bounded provider worker and must never assume a Minecraft
 * owner thread. A provider must complete each returned stage within the caller-supplied deadline.
 * Exceptions complete the stage exceptionally only for implementation defects; expected failures
 * use {@link Result}.</p>
 */
public interface Provider {
    /** @return immutable descriptor available before provider start */
    Descriptor descriptor();
    /** @return current sanitized health snapshot without blocking */
    Health health();
    /** @return asynchronous transition to {@link LifecycleState#RUNNING} */
    CompletionStage<Result<LifecycleState>> start();
    /** @return asynchronous bounded drain result */
    CompletionStage<Result<LifecycleState>> drain(Duration deadline);
    /** @return asynchronous transition to {@link LifecycleState#STOPPED} */
    CompletionStage<Result<LifecycleState>> stop();

    /** Immutable provider identity, version and capability declaration. */
    final class Descriptor {
        private final ProviderId id;
        private final SemanticVersion version;
        private final CapabilitySet capabilities;
        private Descriptor(final ProviderId id, final SemanticVersion version, final CapabilitySet capabilities) {
            this.id = Objects.requireNonNull(id, "id");
            this.version = Objects.requireNonNull(version, "version");
            this.capabilities = Objects.requireNonNull(capabilities, "capabilities");
        }
        /** @return provider descriptor */
        public static Descriptor of(final ProviderId id, final SemanticVersion version,
                                    final CapabilitySet capabilities) {
            return new Descriptor(id, version, capabilities);
        }
        /** @return stable provider ID */ public ProviderId id() { return id; }
        /** @return provider implementation version */ public SemanticVersion version() { return version; }
        /** @return immutable capabilities */ public CapabilitySet capabilities() { return capabilities; }
    }

    /** Immutable, secret-free provider health snapshot. */
    final class Health {
        private final HealthStatus status;
        private final Instant observedAt;
        private final String diagnosticCode;
        private Health(final HealthStatus status, final Instant observedAt, final String diagnosticCode) {
            this.status = Objects.requireNonNull(status, "status");
            this.observedAt = Objects.requireNonNull(observedAt, "observedAt");
            if (diagnosticCode == null || !diagnosticCode.matches("[a-z0-9][a-z0-9_.-]{0,127}")) {
                throw new IllegalArgumentException("diagnosticCode must be a safe stable key");
            }
            this.diagnosticCode = diagnosticCode;
        }
        /** @return sanitized health snapshot */
        public static Health of(final HealthStatus status, final Instant observedAt, final String diagnosticCode) {
            return new Health(status, observedAt, diagnosticCode);
        }
        /** @return health status */ public HealthStatus status() { return status; }
        /** @return observation time */ public Instant observedAt() { return observedAt; }
        /** @return stable diagnostic key with no secret or endpoint data */ public String diagnosticCode() { return diagnosticCode; }
    }

    /** Provider lifecycle state. */
    enum LifecycleState { NEW, STARTING, RUNNING, DRAINING, STOPPED, FAILED }
    /** Sanitized health classification. */
    enum HealthStatus { HEALTHY, DEGRADED, UNAVAILABLE, DISABLED }
}
