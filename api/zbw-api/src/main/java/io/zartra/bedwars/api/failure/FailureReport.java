package io.zartra.bedwars.api.failure;

import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.DefinitionId;
import java.time.Instant;
import java.util.Objects;

/**
 * Immutable secret-safe failure metadata.
 *
 * <p>Raw exception messages, stack traces, endpoints and caller-supplied values are deliberately
 * absent. Consumers localize {@link #messageKey()} and use the correlation ID to find restricted
 * internal evidence.</p>
 */
public final class FailureReport {
    private final DefinitionId code;
    private final FailureKind kind;
    private final CorrelationId correlationId;
    private final String messageKey;
    private final boolean retryable;
    private final Instant observedAt;

    private FailureReport(final DefinitionId code, final FailureKind kind,
                          final CorrelationId correlationId, final String messageKey,
                          final boolean retryable, final Instant observedAt) {
        this.code = Objects.requireNonNull(code, "code");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.correlationId = Objects.requireNonNull(correlationId, "correlationId");
        if (messageKey == null || !messageKey.matches("[a-z0-9][a-z0-9_.-]{0,127}")) {
            throw new IllegalArgumentException("messageKey must be a stable localization key");
        }
        this.messageKey = messageKey;
        this.retryable = retryable;
        this.observedAt = Objects.requireNonNull(observedAt, "observedAt");
    }

    /** @return a validated secret-safe report */
    public static FailureReport of(final DefinitionId code, final FailureKind kind,
                                   final CorrelationId correlationId, final String messageKey,
                                   final boolean retryable, final Instant observedAt) {
        return new FailureReport(code, kind, correlationId, messageKey, retryable, observedAt);
    }

    /** @return stable namespaced failure code */ public DefinitionId code() { return code; }
    /** @return operational classification */ public FailureKind kind() { return kind; }
    /** @return request-spanning correlation identity */ public CorrelationId correlationId() { return correlationId; }
    /** @return localization key, never raw failure text */ public String messageKey() { return messageKey; }
    /** @return whether bounded policy may retry this operation */ public boolean retryable() { return retryable; }
    /** @return immutable observation instant */ public Instant observedAt() { return observedAt; }

    @Override public int hashCode() {
        return Objects.hash(code, kind, correlationId, messageKey, Boolean.valueOf(retryable), observedAt);
    }
    @Override public boolean equals(final Object other) {
        if (this == other) { return true; }
        if (!(other instanceof FailureReport)) { return false; }
        final FailureReport that = (FailureReport) other;
        return retryable == that.retryable && code.equals(that.code) && kind == that.kind
                && correlationId.equals(that.correlationId) && messageKey.equals(that.messageKey)
                && observedAt.equals(that.observedAt);
    }
}
