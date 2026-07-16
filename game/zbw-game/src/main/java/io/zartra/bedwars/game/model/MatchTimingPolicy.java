package io.zartra.bedwars.game.model;

import java.time.Duration;
import java.util.Objects;

/** Immutable timing input separated from arena-derived player and team limits. */
public final class MatchTimingPolicy {
    private final int countdownSeconds;
    private final Duration reconnectGrace;
    private final Duration leaveDelay;
    private final Duration refreshCadence;

    /** Creates bounded timing policy without selecting a game mode. */
    public MatchTimingPolicy(final int countdownSeconds, final Duration reconnectGrace,
                             final Duration leaveDelay, final Duration refreshCadence) {
        if (countdownSeconds < 1 || countdownSeconds > 600) {
            throw new IllegalArgumentException("countdownSeconds must be between 1 and 600");
        }
        this.countdownSeconds = countdownSeconds;
        this.reconnectGrace = positive(reconnectGrace, "reconnectGrace", Duration.ofHours(1));
        this.leaveDelay = positive(leaveDelay, "leaveDelay", Duration.ofMinutes(5));
        this.refreshCadence = positive(refreshCadence, "refreshCadence", Duration.ofSeconds(10));
    }

    private static Duration positive(final Duration value, final String name,
                                     final Duration maximum) {
        Objects.requireNonNull(value, name);
        if (value.isZero() || value.isNegative() || value.compareTo(maximum) > 0) {
            throw new IllegalArgumentException(name + " must be positive and bounded");
        }
        return value;
    }

    /** @return configured initial countdown seconds */ public int countdownSeconds() { return countdownSeconds; }
    /** @return maximum disconnect interval eligible for rejoin */ public Duration reconnectGrace() { return reconnectGrace; }
    /** @return active-match leave delay */ public Duration leaveDelay() { return leaveDelay; }
    /** @return bounded projection refresh cadence */ public Duration refreshCadence() { return refreshCadence; }

    @Override public int hashCode() {
        return Objects.hash(countdownSeconds, reconnectGrace, leaveDelay, refreshCadence);
    }
    @Override public boolean equals(final Object other) {
        if (this == other) { return true; }
        if (!(other instanceof MatchTimingPolicy)) { return false; }
        final MatchTimingPolicy that = (MatchTimingPolicy) other;
        return countdownSeconds == that.countdownSeconds
                && reconnectGrace.equals(that.reconnectGrace)
                && leaveDelay.equals(that.leaveDelay)
                && refreshCadence.equals(that.refreshCadence);
    }
}
