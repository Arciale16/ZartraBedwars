package io.zartra.bedwars.api.result;

import io.zartra.bedwars.api.identity.DefinitionId;
import java.util.Objects;

/** Immutable, localization-safe public error description. */
public final class ApiError {
    private final DefinitionId code;
    private final String messageKey;
    private final RetryDisposition retryDisposition;

    private ApiError(final DefinitionId code, final String messageKey,
                     final RetryDisposition retryDisposition) {
        this.code = Objects.requireNonNull(code, "code");
        if (messageKey == null || !messageKey.matches("[a-z0-9][a-z0-9_.-]{0,127}")) {
            throw new IllegalArgumentException("messageKey must be a stable localization key");
        }
        this.messageKey = messageKey;
        this.retryDisposition = Objects.requireNonNull(retryDisposition, "retryDisposition");
    }

    /** @return a typed error */
    public static ApiError of(final DefinitionId code, final String messageKey,
                              final RetryDisposition retryDisposition) {
        return new ApiError(code, messageKey, retryDisposition);
    }

    /** @return stable namespaced error code */
    public DefinitionId code() { return code; }
    /** @return localization key; never user-controlled text */
    public String messageKey() { return messageKey; }
    /** @return retry classification */
    public RetryDisposition retryDisposition() { return retryDisposition; }

    @Override public int hashCode() { return Objects.hash(code, messageKey, retryDisposition); }
    @Override public boolean equals(final Object other) {
        if (this == other) { return true; }
        if (!(other instanceof ApiError)) { return false; }
        final ApiError that = (ApiError) other;
        return code.equals(that.code) && messageKey.equals(that.messageKey)
                && retryDisposition == that.retryDisposition;
    }

    /** Retry semantics for a typed public failure. */
    public enum RetryDisposition {
        /** Retrying the same operation may succeed after backoff. */
        RETRYABLE,
        /** The request must change before it can succeed. */
        PERMANENT,
        /** The caller is not permitted to retry without new authorization. */
        FORBIDDEN
    }
}
