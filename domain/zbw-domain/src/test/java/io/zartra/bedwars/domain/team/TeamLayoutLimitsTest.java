package io.zartra.bedwars.domain.team;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TeamLayoutLimitsTest {
    @Test void acceptsSharedBoundaryValues() {
        assertEquals(2, TeamLayoutLimits.requireTeamCount(2));
        assertEquals(64, TeamLayoutLimits.requireTeamCount(64));
        assertEquals(1, TeamLayoutLimits.requireTeamCapacity(1));
        assertEquals(64, TeamLayoutLimits.requireTeamCapacity(64));
        assertEquals(256, TeamLayoutLimits.requireMaximumPlayers(256));
    }

    @Test void rejectsValuesOutsideEverySharedBoundary() {
        assertThrows(IllegalArgumentException.class,
                () -> TeamLayoutLimits.requireTeamCount(1));
        assertThrows(IllegalArgumentException.class,
                () -> TeamLayoutLimits.requireTeamCount(65));
        assertThrows(IllegalArgumentException.class,
                () -> TeamLayoutLimits.requireTeamCapacity(0));
        assertThrows(IllegalArgumentException.class,
                () -> TeamLayoutLimits.requireTeamCapacity(65));
        assertThrows(IllegalArgumentException.class,
                () -> TeamLayoutLimits.requireMaximumPlayers(0));
        assertThrows(IllegalArgumentException.class,
                () -> TeamLayoutLimits.requireMaximumPlayers(257));
    }
}
