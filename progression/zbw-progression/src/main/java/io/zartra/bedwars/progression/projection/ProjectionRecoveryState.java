package io.zartra.bedwars.progression.projection;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Immutable bounded retry state for crash recovery. */
public final class ProjectionRecoveryState {
    private final ProgressionEventInput input;
    private final int attempts;
    private final Instant nextAttemptAt;
    private final String lastFailureCode;

    /** Creates recovery state with a positive bounded attempt count. */
    public ProjectionRecoveryState(final ProgressionEventInput input, final int attempts,
                                   final Instant nextAttemptAt, final String lastFailureCode) {
        if (attempts < 1 || attempts > 100) { throw new IllegalArgumentException("attempts must be 1..100"); }
        if (lastFailureCode != null && (lastFailureCode.isEmpty() || lastFailureCode.length() > 64)) { throw new IllegalArgumentException("invalid failure code"); }
        this.input = Objects.requireNonNull(input, "input");
        this.attempts = attempts;
        this.nextAttemptAt = Objects.requireNonNull(nextAttemptAt, "nextAttemptAt");
        this.lastFailureCode = lastFailureCode;
    }
    /** @return original immutable input */ public ProgressionEventInput input() { return input; }
    /** @return completed attempt count */ public int attempts() { return attempts; }
    /** @return earliest permitted retry */ public Instant nextAttemptAt() { return nextAttemptAt; }
    /** @return sanitized last failure code */ public Optional<String> lastFailureCode() { return Optional.ofNullable(lastFailureCode); }
}
