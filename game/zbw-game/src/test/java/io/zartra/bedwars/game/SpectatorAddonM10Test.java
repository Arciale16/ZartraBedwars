package io.zartra.bedwars.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.ArenaId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.game.matchmaking.MatchmakingFramework.Party;
import io.zartra.bedwars.game.matchmaking.MatchmakingFramework.PartyId;
import io.zartra.bedwars.game.model.PlayerStateSnapshot;
import io.zartra.bedwars.game.model.TeamDefinition;
import io.zartra.bedwars.game.selector.M10AddonSelection.Callout;
import io.zartra.bedwars.game.selector.M10AddonSelection.Compass;
import io.zartra.bedwars.game.selector.M10AddonSelection.TeamOption;
import io.zartra.bedwars.game.selector.M10AddonSelection.TeamSelector;
import io.zartra.bedwars.game.selector.M10AddonSelection.TeamVerdict;
import io.zartra.bedwars.game.selector.M10AddonSelection.TrackerTarget;
import io.zartra.bedwars.game.spectator.SpectatorFramework;
import io.zartra.bedwars.game.spectator.SpectatorFramework.EntryReason;
import io.zartra.bedwars.game.spectator.SpectatorFramework.Policy;
import io.zartra.bedwars.game.spectator.SpectatorFramework.Preferences;
import io.zartra.bedwars.game.spectator.SpectatorFramework.Restrictions;
import io.zartra.bedwars.game.spectator.SpectatorFramework.Service;
import io.zartra.bedwars.game.spectator.SpectatorFramework.Target;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SpectatorAddonM10Test {
    private static final Instant NOW = Instant.parse("2026-07-17T12:00:00Z");
    private static final MatchId MATCH = MatchId.of(new UUID(1L, 1L));

    @Test void eliminatedSpectatorEntersIdempotentlyAndRestoresExactlyOnce() {
        final Service service = service(true, new ArrayList<SpectatorFramework.Event>());
        final PlayerStateSnapshot captured = state(1);
        final SpectatorFramework.Session session = service.enter(MATCH, EntryReason.ELIMINATED, captured);
        assertEquals(session.id(), service.enter(MATCH, EntryReason.ELIMINATED, captured).id());
        final SpectatorFramework.Restoration restoration = service.leave(captured.playerId(), 0L);
        assertTrue(service.restored(captured.playerId(), restoration.sessionId(), restoration.sessionRevision()));
        assertFalse(service.restored(captured.playerId(), restoration.sessionId(), restoration.sessionRevision()));
        assertEquals(0, service.activeSessions());
    }

    @Test void externalAdmissionAndCapacityFailClosed() {
        final Service denied = service(false, new ArrayList<SpectatorFramework.Event>());
        assertThrows(SecurityException.class, () -> denied.enter(MATCH, EntryReason.EXTERNAL, state(1)));
        final Service bounded = new Service(new Policy(1, Duration.ofSeconds(30), true, restrictions()),
                TimeSource.FixedTimeSource.at(NOW), event -> { });
        bounded.enter(MATCH, EntryReason.EXTERNAL, state(1));
        assertThrows(IllegalStateException.class, () -> bounded.enter(MATCH, EntryReason.EXTERNAL, state(2)));
    }

    @Test void targetNavigationRejectsHiddenVanishedDeadAndOtherMatchPlayers() {
        final Service service = service(true, new ArrayList<SpectatorFramework.Event>());
        final SpectatorFramework.Session session = service.enter(MATCH, EntryReason.ELIMINATED, state(1));
        assertThrows(IllegalArgumentException.class, () -> service.target(player(1), session.revision(), target(2, MATCH, false, true, false, true, 0)));
        assertThrows(IllegalArgumentException.class, () -> service.target(player(1), session.revision(), target(2, MATCH, true, false, false, true, 0)));
        assertThrows(IllegalArgumentException.class, () -> service.target(player(1), session.revision(), target(2, MATCH, true, true, true, true, 0)));
        final MatchId other = MatchId.of(new UUID(1L, 2L));
        assertThrows(IllegalArgumentException.class, () -> service.target(player(1), session.revision(), target(2, other, true, true, false, true, 0)));
    }

    @Test void nextPreviousAndUnavailableTargetAreDeterministic() {
        final Service service = service(true, new ArrayList<SpectatorFramework.Event>());
        SpectatorFramework.Session session = service.enter(MATCH, EntryReason.ELIMINATED, state(1));
        final List<Target> targets = Arrays.asList(target(3, MATCH, true, true, false, true, 2),
                target(2, MATCH, true, true, false, true, 1));
        session = service.navigate(player(1), session.revision(), targets, true);
        assertEquals(player(2), session.target().get());
        session = service.navigate(player(1), session.revision(), targets, false);
        assertEquals(player(3), session.target().get());
        session = service.targetUnavailable(player(1), session.revision(), player(3));
        assertFalse(session.target().isPresent());
    }

    @Test void disconnectReconnectPreservesOnlyValidTarget() {
        final Service service = service(true, new ArrayList<SpectatorFramework.Event>());
        SpectatorFramework.Session session = service.enter(MATCH, EntryReason.ELIMINATED, state(1));
        session = service.target(player(1), session.revision(), target(2, MATCH, true, true, false, true, 0));
        session = service.disconnect(player(1), session.revision());
        session = service.reconnect(player(1), session.revision(), Collections.singleton(target(2, MATCH, false, true, false, true, 0)));
        assertFalse(session.target().isPresent());
        assertThrows(IllegalStateException.class, () -> service.reconnect(player(1), 0L, Collections.<Target>emptyList()));
    }

    @Test void preferencesPersistButTargetStateDoesNot() {
        final Service service = service(true, new ArrayList<SpectatorFramework.Event>());
        SpectatorFramework.Session session = service.enter(MATCH, EntryReason.ELIMINATED, state(1));
        session = service.preferences(player(1), session.revision(), new Preferences(9, true, false, true));
        final SpectatorFramework.Restoration restoration = service.leave(player(1), session.revision());
        service.restored(player(1), restoration.sessionId(), restoration.sessionRevision());
        final SpectatorFramework.Session next = service.enter(MATCH, EntryReason.EXTERNAL, state(1));
        assertEquals(9, next.preferences().flightSpeedLevel());
        assertFalse(next.target().isPresent());
    }

    @Test void restrictionsPreserveFlightWhileDenyingUnsafeInteractions() {
        final Restrictions restrictions = service(true, new ArrayList<SpectatorFramework.Event>()).restrictions();
        assertTrue(restrictions.flight());
        assertTrue(restrictions.teleport());
        assertFalse(restrictions.canDropItems());
        assertFalse(restrictions.canPickupItems());
        assertFalse(restrictions.canTakeDamage());
        assertFalse(restrictions.canChangeWorld());
    }

    @Test void matchEndProducesRestorationForEveryBoundSession() {
        final Service service = service(true, new ArrayList<SpectatorFramework.Event>());
        service.enter(MATCH, EntryReason.ELIMINATED, state(1));
        service.enter(MATCH, EntryReason.STAFF, state(2));
        assertEquals(2, service.matchEnded(MATCH).size());
    }

    @Test void teamSelectorKeepsPartyTogetherAndRejectsFullTeam() {
        final TeamSelector selector = new TeamSelector(32);
        final Party party = party(1, 3, 4L);
        final TeamOption available = option("red", 1, 4, true, true);
        assertEquals(TeamVerdict.ACCEPTED, selector.select(arena(1), 2L, party.leader(), party, available, false));
        assertEquals(3, selector.size());
        assertEquals(TeamVerdict.FULL, selector.select(arena(1), 2L, player(9), null,
                option("blue", 4, 4, true, true), false));
        assertEquals(TeamVerdict.LOCKED, selector.select(arena(1), 2L, player(9), null, available, true));
    }

    @Test void teamSelectorSupportsTwelveTeamsAndStaleReconnectCleanup() {
        final TeamSelector selector = new TeamSelector(64);
        final List<TeamOption> options = new ArrayList<TeamOption>();
        for (int index = 0; index < 12; index++) { options.add(option("semantic-" + index, index % 2, 3, true, true)); }
        assertEquals(12, options.size());
        final TeamOption selected = selector.autoAssign(options, 2);
        assertEquals(0, selected.occupied());
        selector.select(arena(1), 3L, player(1), null, selected, false);
        assertFalse(selector.reconnect(player(1), 4L, 0L).isPresent());
        assertEquals(0, selector.size());
    }

    @Test void compassChoosesNearestSafeEnemyAndDoesNotLeakHiddenTarget() {
        final Compass compass = new Compass(TimeSource.FixedTimeSource.at(NOW), Duration.ofSeconds(2), 100D, 10);
        final DefinitionId ownTeam = DefinitionId.of("team", "red");
        final TrackerTarget hidden = tracker(2, "blue", 1D, true, false, false, false);
        final TrackerTarget enemy = tracker(3, "blue", 10D, true, true, false, false);
        final TrackerTarget own = tracker(4, "red", 2D, true, true, false, false);
        assertEquals(player(3), compass.nearest(ownTeam, Arrays.asList(hidden, enemy, own)).get().playerId());
        assertFalse(compass.callout(player(1), ownTeam, Callout.DANGER, hidden).safeTarget().isPresent());
        assertThrows(IllegalStateException.class, () -> compass.callout(player(1), ownTeam, Callout.ATTACK, enemy));
    }

    @Test void malformedSpectatorAndAddonInputsFailClosed() {
        assertThrows(IllegalArgumentException.class, () -> new Preferences(11, false, false, false));
        assertThrows(IllegalArgumentException.class, () -> new Policy(0, Duration.ofSeconds(1), true, restrictions()));
        assertThrows(IllegalArgumentException.class, () -> new TeamSelector(0));
        assertThrows(IllegalArgumentException.class, () -> new Compass(TimeSource.FixedTimeSource.at(NOW), Duration.ZERO, 1D, 1));
    }

    private static Service service(final boolean external, final List<SpectatorFramework.Event> events) {
        return new Service(new Policy(16, Duration.ofSeconds(30), external, restrictions()),
                TimeSource.FixedTimeSource.at(NOW), events::add);
    }
    private static Restrictions restrictions() {
        return new Restrictions(true, true, false,
                Collections.singleton(DefinitionId.of("zartra", "command/spectator-leave")));
    }
    private static PlayerStateSnapshot state(final int seed) {
        return new PlayerStateSnapshot(player(seed), PlayerStateSnapshot.Inventory.empty(36),
                new PlayerStateSnapshot.Location(DefinitionId.of("zartra", "world/lobby"), 0, 70, 0, 0, 0),
                PlayerStateSnapshot.Mode.ADVENTURE, true);
    }
    private static Target target(final int seed, final MatchId match, final boolean living,
                                 final boolean visible, final boolean vanished,
                                 final boolean consent, final int order) {
        return new Target(player(seed), match, DefinitionId.of("team", "team-" + seed), living,
                visible, vanished, consent, order);
    }
    private static Party party(final int seed, final int size, final long revision) {
        final List<PlayerId> members = new ArrayList<PlayerId>();
        for (int index = 0; index < size; index++) { members.add(player(seed * 10 + index)); }
        return new Party(PartyId.of(new UUID(0, seed)), members.get(0), members, revision);
    }
    private static TeamOption option(final String id, final int occupied, final int capacity,
                                     final boolean enabled, final boolean permitted) {
        return new TeamOption(TeamDefinition.of(DefinitionId.of("team", id), "Team " + id,
                DefinitionId.of("color", id), capacity), occupied, enabled, permitted);
    }
    private static TrackerTarget tracker(final int seed, final String team, final double distance,
                                         final boolean living, final boolean visible,
                                         final boolean vanished, final boolean spectator) {
        return new TrackerTarget(player(seed), DefinitionId.of("team", team), distance, living,
                visible, vanished, spectator);
    }
    private static PlayerId player(final int seed) { return PlayerId.of(new UUID(0L, seed)); }
    private static ArenaId arena(final int seed) { return ArenaId.of(new UUID(1L, seed)); }
}
