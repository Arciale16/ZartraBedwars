package io.zartra.bedwars.paper.replay.viewer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.zartra.bedwars.replay.api.ReplayId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** ZBW-REPLAY-004/007 immutable replay UX and bounded-control tests. */
final class ReplayUxModelTest {
    private static final UUID VIEWER = new UUID(0L, 1708L);
    private static final ReplayId REPLAY =
            ReplayId.parse("00000000-0000-0000-0000-000000001708");

    @Test
    void supportsOnlyTheFiveExactPlaybackSpeeds() {
        assertEquals(ReplayViewerSpeed.QUARTER, ReplayViewerSpeed.parse("0.25x"));
        assertEquals(ReplayViewerSpeed.HALF, ReplayViewerSpeed.parse("0.5"));
        assertEquals(ReplayViewerSpeed.NORMAL, ReplayViewerSpeed.parse("1x"));
        assertEquals(ReplayViewerSpeed.DOUBLE, ReplayViewerSpeed.parse("2"));
        assertEquals(ReplayViewerSpeed.QUADRUPLE, ReplayViewerSpeed.parse("4x"));
        assertThrows(IllegalArgumentException.class, () -> ReplayViewerSpeed.parse("0.75"));
        assertThrows(IllegalArgumentException.class, () -> ReplayViewerSpeed.parse(null));
    }

    @Test
    void menuStateIsBoundedAndDefensivelyImmutable() {
        final ReplayViewerSession viewer = ReplayViewerSession.connected(VIEWER, REPLAY)
                .start().changeSpeed(ReplayViewerSpeed.DOUBLE);
        final ReplayInformationPanel information = new ReplayInformationPanel(
                REPLAY, 10L, 20L, ReplayViewerSpeed.DOUBLE, ViewerState.WATCHING, true);
        final List<String> players = new ArrayList<String>(Collections.singletonList("player"));
        final List<ReplayMenuEvent> events = new ArrayList<ReplayMenuEvent>(
                Collections.singletonList(new ReplayMenuEvent(0, 10L, "player.kill")));
        final ReplayMenuState menu = new ReplayMenuState(
                VIEWER, viewer, information, players, events);
        players.clear();
        events.clear();

        assertEquals(1, menu.players().size());
        assertEquals(1, menu.importantEvents().size());
        assertEquals(10L, menu.information().currentMillis());
        assertEquals(20L, menu.information().durationMillis());
        assertThrows(UnsupportedOperationException.class, () -> menu.players().add("other"));
        assertThrows(IllegalArgumentException.class, () -> new ReplayInformationPanel(
                REPLAY, 21L, 20L, ReplayViewerSpeed.NORMAL, ViewerState.WATCHING, false));
    }

    @Test
    void menuRowsValidateAndImplementValueSemantics() {
        final ReplayMenuEvent event = new ReplayMenuEvent(0, 10L, "player.kill");
        assertEquals(event, new ReplayMenuEvent(0, 10L, "player.kill"));
        assertEquals(event.hashCode(), new ReplayMenuEvent(0, 10L, "player.kill").hashCode());
        assertFalse(event.equals(new ReplayMenuEvent(1, 10L, "player.kill")));
        assertFalse(event.equals(new ReplayMenuEvent(0, 11L, "player.kill")));
        assertFalse(event.equals(new ReplayMenuEvent(0, 10L, "bed.destroyed")));
        assertFalse(event.equals("event"));
        assertThrows(IllegalArgumentException.class, () -> new ReplayMenuEvent(-1, 0L, "x"));
        assertThrows(IllegalArgumentException.class, () -> new ReplayMenuEvent(0, -1L, "x"));
        assertThrows(IllegalArgumentException.class, () -> new ReplayMenuEvent(0, 0L, null));
        assertThrows(IllegalArgumentException.class, () -> new ReplayMenuEvent(0, 0L, " "));
    }

    @Test
    void menuRejectsMismatchedIdentityNullsAndOversizedLists() {
        final ReplayViewerSession viewer = ReplayViewerSession.connected(VIEWER, REPLAY).start();
        final ReplayInformationPanel information = new ReplayInformationPanel(
                REPLAY, 0L, 0L, ReplayViewerSpeed.NORMAL, ViewerState.WATCHING, false);
        assertThrows(IllegalArgumentException.class, () -> new ReplayMenuState(
                UUID.randomUUID(), viewer, information, Collections.emptyList(),
                Collections.emptyList()));
        assertThrows(IllegalArgumentException.class, () -> new ReplayMenuState(
                VIEWER, viewer, information, Collections.singletonList(null),
                Collections.emptyList()));
        final List<String> oversized = new ArrayList<String>();
        for (int index = 0; index < 129; index++) { oversized.add("p" + index); }
        assertThrows(IllegalArgumentException.class, () -> new ReplayMenuState(
                VIEWER, viewer, information, oversized, Collections.emptyList()));
        assertThrows(NullPointerException.class, () -> ReplayViewerSpeed.fromPlayback(null));
        assertThrows(IllegalArgumentException.class, () -> new ReplayInformationPanel(
                REPLAY, -1L, 0L, ReplayViewerSpeed.NORMAL, ViewerState.WATCHING, false));
    }}
