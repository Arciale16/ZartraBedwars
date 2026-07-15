package io.zartra.bedwars.storage.api;

import java.time.Duration;
import java.util.Objects;

/** Immutable transaction access, timeout and retry policy. */
public final class TransactionOptions {
    private final AccessMode accessMode;
    private final Duration timeout;
    private final int deadlockRetries;

    private TransactionOptions(final AccessMode accessMode, final Duration timeout,
                               final int deadlockRetries) {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        if (deadlockRetries < 0 || deadlockRetries > 16) {
            throw new IllegalArgumentException("deadlockRetries must be between 0 and 16");
        }
        this.accessMode = Objects.requireNonNull(accessMode, "accessMode");
        this.timeout = timeout;
        this.deadlockRetries = deadlockRetries;
    }

    /** @return validated options */
    public static TransactionOptions of(final AccessMode accessMode, final Duration timeout,
                                        final int deadlockRetries) {
        return new TransactionOptions(accessMode, timeout, deadlockRetries);
    }

    /** @return read/write intent */ public AccessMode accessMode() { return accessMode; }
    /** @return operation timeout */ public Duration timeout() { return timeout; }
    /** @return bounded deadlock retry count */ public int deadlockRetries() { return deadlockRetries; }

    /** Transaction access mode. */
    public enum AccessMode {
        /** No mutation is permitted. */ READ_ONLY,
        /** Atomic durable mutation is permitted. */ READ_WRITE
    }
}
