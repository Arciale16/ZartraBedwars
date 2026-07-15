package io.zartra.bedwars.arena.application;

import java.time.Duration;
import java.util.Objects;

/** Immutable typed configuration foundation for arena/setup admission and timeouts. */
public final class ArenaPolicy {
    private final int maximumArenas;
    private final int maximumConcurrentSetupSessions;
    private final Duration worldOperationTimeout;
    private final Duration repositoryOperationTimeout;

    private ArenaPolicy(final int maximumArenas, final int maximumConcurrentSetupSessions,
                        final Duration worldOperationTimeout,
                        final Duration repositoryOperationTimeout) {
        if (maximumArenas < 1 || maximumArenas > 10000) {
            throw new IllegalArgumentException("maximumArenas must be between 1 and 10000");
        }
        if (maximumConcurrentSetupSessions < 1 || maximumConcurrentSetupSessions > 256) {
            throw new IllegalArgumentException("maximumConcurrentSetupSessions must be between 1 and 256");
        }
        this.maximumArenas = maximumArenas;
        this.maximumConcurrentSetupSessions = maximumConcurrentSetupSessions;
        this.worldOperationTimeout = positive(worldOperationTimeout, "worldOperationTimeout");
        this.repositoryOperationTimeout = positive(repositoryOperationTimeout, "repositoryOperationTimeout");
    }
    /** @return validated policy */
    public static ArenaPolicy of(final int maximumArenas,
                                 final int maximumConcurrentSetupSessions,
                                 final Duration worldOperationTimeout,
                                 final Duration repositoryOperationTimeout) {
        return new ArenaPolicy(maximumArenas, maximumConcurrentSetupSessions,
                worldOperationTimeout, repositoryOperationTimeout);
    }
    private static Duration positive(final Duration value, final String label) {
        final Duration checked = Objects.requireNonNull(value, label);
        if (checked.isZero() || checked.isNegative() || checked.compareTo(Duration.ofMinutes(10)) > 0) {
            throw new IllegalArgumentException(label + " must be positive and at most ten minutes");
        }
        return checked;
    }
    /** @return configured arena inventory bound */ public int maximumArenas() { return maximumArenas; }
    /** @return setup-session admission bound */ public int maximumConcurrentSetupSessions() { return maximumConcurrentSetupSessions; }
    /** @return world operation deadline */ public Duration worldOperationTimeout() { return worldOperationTimeout; }
    /** @return repository operation deadline for adapters */ public Duration repositoryOperationTimeout() { return repositoryOperationTimeout; }
}
