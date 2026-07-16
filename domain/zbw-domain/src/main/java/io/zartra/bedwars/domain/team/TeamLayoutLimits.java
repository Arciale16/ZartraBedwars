package io.zartra.bedwars.domain.team;

/**
 * Authoritative platform-neutral bounds for arena and runtime team layouts.
 *
 * <p>The bounds are safety ceilings, not a list of supported presets. Arena definitions may
 * choose any validated team count and capacity inside them.</p>
 */
public final class TeamLayoutLimits {
    /** Smallest playable team count. */
    public static final int MINIMUM_TEAM_COUNT = 2;
    /** Largest configurable team count retained by neutral models. */
    public static final int MAXIMUM_TEAM_COUNT = 64;
    /** Smallest per-team player capacity. */
    public static final int MINIMUM_TEAM_CAPACITY = 1;
    /** Largest per-team player capacity retained by neutral models. */
    public static final int MAXIMUM_TEAM_CAPACITY = 64;
    /** Largest admitted-player bound for one match allocation. */
    public static final int MAXIMUM_MATCH_PLAYERS = 256;

    private TeamLayoutLimits() {
        throw new AssertionError("No instances");
    }

    /**
     * Validates a playable team count.
     *
     * @param value configured team count
     * @return the validated value
     * @throws IllegalArgumentException when outside the shared bounds
     */
    public static int requireTeamCount(final int value) {
        if (value < MINIMUM_TEAM_COUNT || value > MAXIMUM_TEAM_COUNT) {
            throw new IllegalArgumentException("team count must be between 2 and 64");
        }
        return value;
    }

    /**
     * Validates one team's capacity.
     *
     * @param value configured capacity
     * @return the validated value
     * @throws IllegalArgumentException when outside the shared bounds
     */
    public static int requireTeamCapacity(final int value) {
        if (value < MINIMUM_TEAM_CAPACITY || value > MAXIMUM_TEAM_CAPACITY) {
            throw new IllegalArgumentException("team capacity must be between 1 and 64");
        }
        return value;
    }

    /**
     * Validates a match-wide admitted-player bound.
     *
     * @param value configured maximum
     * @return the validated value
     * @throws IllegalArgumentException when outside the shared bounds
     */
    public static int requireMaximumPlayers(final int value) {
        if (value < 1 || value > MAXIMUM_MATCH_PLAYERS) {
            throw new IllegalArgumentException("maximum players must be between 1 and 256");
        }
        return value;
    }
}
