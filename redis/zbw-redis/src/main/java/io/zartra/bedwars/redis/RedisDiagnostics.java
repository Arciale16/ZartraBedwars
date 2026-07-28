package io.zartra.bedwars.redis;

import io.zartra.bedwars.redis.api.DegradationMode;
import io.zartra.bedwars.redis.api.RedisAvailability;
import io.zartra.bedwars.redis.api.RedisHealth;
import java.time.Instant;
import java.util.Objects;

/** Sanitized immutable operational view with no endpoint, credential or domain payload. */
public final class RedisDiagnostics {
    private final RedisAvailability availability;
    private final DegradationMode degradationMode;
    private final RedisCircuitBreaker.State circuitState;
    private final String diagnosticCode;
    private final int pendingOperations;
    private final int deduplicationEntries;
    private final int coordinationMetadataEntries;
    private final Instant observedAt;

    /** Creates a bounded sanitized diagnostic snapshot. */
    public RedisDiagnostics(final RedisHealth health,
                            final RedisCircuitBreaker.State circuitState,
                            final int deduplicationEntries,
                            final int coordinationMetadataEntries) {
        final RedisHealth checked = Objects.requireNonNull(health, "health");
        this.availability = checked.availability();
        this.degradationMode = checked.mode();
        this.circuitState = Objects.requireNonNull(circuitState, "circuitState");
        this.diagnosticCode = checked.diagnosticCode();
        this.pendingOperations = checked.pendingOperations();
        if (deduplicationEntries < 0 || deduplicationEntries > RedisDeduplicationStore.MAX_ENTRIES
                || coordinationMetadataEntries < 0 || coordinationMetadataEntries > 100000) {
            throw new IllegalArgumentException("diagnostic counts outside bounded limits");
        }
        this.deduplicationEntries = deduplicationEntries;
        this.coordinationMetadataEntries = coordinationMetadataEntries;
        this.observedAt = checked.observedAt();
    }

    /** @return sanitized availability */ public RedisAvailability availability() {
        return availability;
    }
    /** @return safe degradation policy */ public DegradationMode degradationMode() {
        return degradationMode;
    }
    /** @return circuit state */ public RedisCircuitBreaker.State circuitState() {
        return circuitState;
    }
    /** @return non-sensitive diagnostic code */ public String diagnosticCode() {
        return diagnosticCode;
    }
    /** @return bounded queued operations */ public int pendingOperations() {
        return pendingOperations;
    }
    /** @return local replay-guard entries */ public int deduplicationEntries() {
        return deduplicationEntries;
    }
    /** @return version metadata entries */ public int coordinationMetadataEntries() {
        return coordinationMetadataEntries;
    }
    /** @return observation timestamp */ public Instant observedAt() { return observedAt; }
}
