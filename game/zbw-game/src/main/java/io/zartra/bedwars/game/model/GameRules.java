package io.zartra.bedwars.game.model;

import java.time.Duration;
import io.zartra.bedwars.domain.team.TeamLayoutLimits;

/** Immutable validated rules for one standard M08 match runtime. */
public final class GameRules {
    private final int minimumPlayers;
    private final int maximumPlayers;
    private final MatchTimingPolicy timing;

    /** Creates bounded rules without selecting an M10 game mode. */
    public GameRules(final int minimumPlayers, final int maximumPlayers,
                     final int countdownSeconds, final Duration reconnectGrace,
                     final Duration leaveDelay, final Duration refreshCadence) {
        this(minimumPlayers, maximumPlayers, new MatchTimingPolicy(countdownSeconds,
                reconnectGrace, leaveDelay, refreshCadence));
    }

    /** Creates arena-bounded rules from an independent timing policy. */
    public GameRules(final int minimumPlayers, final int maximumPlayers,
                     final MatchTimingPolicy timing) {
        TeamLayoutLimits.requireMaximumPlayers(maximumPlayers);
        if (minimumPlayers < 1 || maximumPlayers < minimumPlayers) {
            throw new IllegalArgumentException("player limits must satisfy 1 <= min <= max <= 256");
        }
        this.minimumPlayers = minimumPlayers;
        this.maximumPlayers = maximumPlayers;
        this.timing = java.util.Objects.requireNonNull(timing, "timing");
    }

    /** @return minimum admitted players required to begin countdown */
    public int minimumPlayers() { return minimumPlayers; }
    /** @return hard per-match admission bound */
    public int maximumPlayers() { return maximumPlayers; }
    /** @return configured initial countdown seconds */
    public int countdownSeconds() { return timing.countdownSeconds(); }
    /** @return maximum disconnect interval eligible for rejoin */
    public Duration reconnectGrace() { return timing.reconnectGrace(); }
    /** @return standard active-match leave delay */
    public Duration leaveDelay() { return timing.leaveDelay(); }
    /** @return bounded projection refresh cadence */
    public Duration refreshCadence() { return timing.refreshCadence(); }
    /** @return immutable timing policy independent of arena player limits */
    public MatchTimingPolicy timing() { return timing; }
}
