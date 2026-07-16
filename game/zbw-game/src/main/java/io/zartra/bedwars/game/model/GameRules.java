package io.zartra.bedwars.game.model;

import java.time.Duration;
import java.util.Objects;

/** Immutable validated rules for one standard M08 match runtime. */
public final class GameRules {
    private final int minimumPlayers;
    private final int maximumPlayers;
    private final int countdownSeconds;
    private final Duration reconnectGrace;
    private final Duration leaveDelay;
    private final Duration refreshCadence;

    /** Creates bounded rules without selecting an M10 game mode. */
    public GameRules(final int minimumPlayers, final int maximumPlayers,
                     final int countdownSeconds, final Duration reconnectGrace,
                     final Duration leaveDelay, final Duration refreshCadence) {
        if (minimumPlayers < 1 || maximumPlayers < minimumPlayers || maximumPlayers > 256) {
            throw new IllegalArgumentException("player limits must satisfy 1 <= min <= max <= 256");
        }
        if (countdownSeconds < 1 || countdownSeconds > 600) {
            throw new IllegalArgumentException("countdownSeconds must be between 1 and 600");
        }
        this.minimumPlayers = minimumPlayers;
        this.maximumPlayers = maximumPlayers;
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

    /** @return minimum admitted players required to begin countdown */
    public int minimumPlayers() { return minimumPlayers; }
    /** @return hard per-match admission bound */
    public int maximumPlayers() { return maximumPlayers; }
    /** @return configured initial countdown seconds */
    public int countdownSeconds() { return countdownSeconds; }
    /** @return maximum disconnect interval eligible for rejoin */
    public Duration reconnectGrace() { return reconnectGrace; }
    /** @return standard active-match leave delay */
    public Duration leaveDelay() { return leaveDelay; }
    /** @return bounded projection refresh cadence */
    public Duration refreshCadence() { return refreshCadence; }
}
