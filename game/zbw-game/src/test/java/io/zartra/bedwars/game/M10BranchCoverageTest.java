package io.zartra.bedwars.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.ArenaId;
import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.localization.MessageKey;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.game.matchmaking.MatchmakingFramework;
import io.zartra.bedwars.game.mode.ModeFramework;
import io.zartra.bedwars.game.model.PlayerStateSnapshot;
import io.zartra.bedwars.game.model.TeamDefinition;
import io.zartra.bedwars.game.selector.M10AddonSelection;
import io.zartra.bedwars.game.selector.SelectorFramework;
import io.zartra.bedwars.game.spectator.SpectatorFramework;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/** Exercises fail-closed and terminal M10 branches that are security-relevant but uncommon. */
class M10BranchCoverageTest {
    private static final Instant NOW = Instant.parse("2026-07-17T12:00:00Z");
    private static final ModeFramework.ModeId MODE = ModeFramework.ModeId.of("zartra", "standard");
    private static final DefinitionId LAYOUT_ID = DefinitionId.of("zartra", "layout/solo");

    @Test void modeValidationAndRegistryTerminalBranches() {
        assertThrows(AssertionError.class, () -> construct(ModeFramework.class));
        assertThrows(IllegalArgumentException.class, () -> new ModeFramework.Version(0, -1));
        assertTrue(new ModeFramework.Version(2, 0).compareTo(new ModeFramework.Version(1, 99)) > 0);
        assertTrue(new ModeFramework.Version(1, 2).compareTo(new ModeFramework.Version(1, 1)) > 0);
        assertFalse(new ModeFramework.Version(1, 2).equals("1.2"));
        assertThrows(IllegalArgumentException.class, () -> new ModeFramework.ConfigField(
                DefinitionId.of("zartra", "field/x"), ModeFramework.FieldType.INTEGER, null, true));
        assertThrows(IllegalArgumentException.class, () -> new ModeFramework.ConfigField(
                DefinitionId.of("zartra", "field/x"), ModeFramework.FieldType.INTEGER,
                repeat('x', 257), true));
        assertThrows(IllegalArgumentException.class, () -> new ModeFramework.ConfigField(
                DefinitionId.of("zartra", "field/x"), ModeFramework.FieldType.INTEGER, "bad\r", true));
        assertThrows(IllegalArgumentException.class, () -> new ModeFramework.DeferredBinding("bad", "M11"));
        assertThrows(IllegalArgumentException.class, () -> new ModeFramework.DeferredBinding("ZBW-GAME-004", "M25"));
        final ModeFramework.Layout layout = layout(8, 1);
        assertThrows(IllegalArgumentException.class, () -> new ModeFramework.Layout(LAYOUT_ID,
                Arrays.asList(team(1), team(1)), 1));
        final ModeFramework.Definition disabled = definition(false, 2, 4, 2, 16,
                Collections.<DefinitionId>emptyList(), Collections.<ModeFramework.ConfigField>emptyList(),
                Collections.<ModeFramework.DeferredBinding>emptyList());
        assertFalse(disabled.supports(layout));
        assertFalse(definition(true, 9, 10, 2, 16, Collections.<DefinitionId>emptyList(),
                Collections.<ModeFramework.ConfigField>emptyList(), Collections.<ModeFramework.DeferredBinding>emptyList()).supports(layout));
        assertThrows(IllegalArgumentException.class, () -> definition(true, 1, 64, 2, 256,
                Collections.<DefinitionId>emptyList(), Collections.<ModeFramework.ConfigField>emptyList(),
                Collections.<ModeFramework.DeferredBinding>emptyList()));
        assertThrows(IllegalArgumentException.class, () -> definition(true, 4, 2, 2, 256,
                Collections.<DefinitionId>emptyList(), Collections.<ModeFramework.ConfigField>emptyList(),
                Collections.<ModeFramework.DeferredBinding>emptyList()));
        final DefinitionId capability = DefinitionId.of("zartra", "capability/x");
        assertThrows(IllegalArgumentException.class, () -> definition(true, 2, 64, 2, 256,
                Arrays.asList(capability, capability), Collections.<ModeFramework.ConfigField>emptyList(),
                Collections.<ModeFramework.DeferredBinding>emptyList()));
        final ModeFramework.ConfigField field = new ModeFramework.ConfigField(
                DefinitionId.of("zartra", "field/x"), ModeFramework.FieldType.BOOLEAN, "true", true);
        assertThrows(IllegalArgumentException.class, () -> definition(true, 2, 64, 2, 256,
                Collections.<DefinitionId>emptyList(), Arrays.asList(field, field),
                Collections.<ModeFramework.DeferredBinding>emptyList()));
        final ModeFramework.DeferredBinding deferred = new ModeFramework.DeferredBinding("ZBW-GAME-004", "M11");
        assertThrows(IllegalArgumentException.class, () -> definition(true, 2, 64, 2, 256,
                Collections.<DefinitionId>emptyList(), Collections.<ModeFramework.ConfigField>emptyList(),
                Arrays.asList(deferred, deferred)));
        final ModeFramework.Registry registry = new ModeFramework.Registry(1, event -> { });
        assertFalse(registry.find(MODE).isPresent());
        assertThrows(IllegalArgumentException.class, () -> registry.require(MODE));
        registry.register(definition(true, 2, 64, 2, 256, Collections.<DefinitionId>emptyList(),
                Collections.<ModeFramework.ConfigField>emptyList(), Collections.<ModeFramework.DeferredBinding>emptyList()));
        assertThrows(IllegalStateException.class, () -> registry.register(definition(true, 2, 64, 2, 256,
                Collections.<DefinitionId>emptyList(), Collections.<ModeFramework.ConfigField>emptyList(),
                Collections.<ModeFramework.DeferredBinding>emptyList())));
    }

    @Test void selectorEveryExclusionAndInputBoundary() {
        assertThrows(AssertionError.class, () -> construct(SelectorFramework.class));
        final ModeFramework.Layout layout = layout(8, 1);
        final SelectorFramework.Candidate ready = candidate(1, layout, true, true, true,
                SelectorFramework.Lifecycle.COUNTDOWN, 0, 0, 2);
        assertEquals(SelectorFramework.Exclusion.MODE_INCOMPATIBLE,
                ready.exclusion(ModeFramework.ModeId.of("zartra", "other"), null, 1, 0));
        assertEquals(SelectorFramework.Exclusion.LAYOUT_INCOMPATIBLE,
                ready.exclusion(MODE, DefinitionId.of("zartra", "layout/other"), 1, 0));
        assertEquals(SelectorFramework.Exclusion.FULL,
                candidate(2, layout, true, true, true, SelectorFramework.Lifecycle.WAITING, 8, 0, 2)
                        .exclusion(MODE, LAYOUT_ID, 1, 0));
        assertEquals(SelectorFramework.Exclusion.WORLD_UNAVAILABLE,
                candidate(3, layout, true, true, false, SelectorFramework.Lifecycle.WAITING, 0, 0, 2)
                        .exclusion(null, null, 1, 0));
        assertEquals(SelectorFramework.Exclusion.CLOSED,
                candidate(4, layout, true, true, true, SelectorFramework.Lifecycle.PLAYING, 0, 0, 2)
                        .exclusion(null, null, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> candidate(5, layout, true, true, true,
                SelectorFramework.Lifecycle.WAITING, -1, 0, 2));
        assertThrows(IllegalArgumentException.class, () -> candidate(5, layout, true, true, true,
                SelectorFramework.Lifecycle.WAITING, 0, -1, 2));
        assertThrows(IllegalArgumentException.class, () -> candidate(5, layout, true, true, true,
                SelectorFramework.Lifecycle.WAITING, 8, 1, 2));
        assertThrows(IllegalArgumentException.class, () -> candidate(5, layout, true, true, true,
                SelectorFramework.Lifecycle.WAITING, 0, 0, -1));
        invalidQuery(null, 1, 0, 0, 10);
        invalidQuery(repeat('x', 129), 1, 0, 0, 10);
        invalidQuery("bad\r", 1, 0, 0, 10);
        invalidQuery("bad\n", 1, 0, 0, 10);
        invalidQuery("", 0, 0, 0, 10);
        invalidQuery("", 257, 0, 0, 10);
        invalidQuery("", 1, -1, 0, 10);
        invalidQuery("", 1, 0, -1, 10);
        invalidQuery("", 1, 0, 10001, 10);
        invalidQuery("", 1, 0, 0, 0);
        invalidQuery("", 1, 0, 0, 46);
        final SelectorFramework.Service service = new SelectorFramework.Service();
        final SelectorFramework.Query mapFilter = new SelectorFramework.Query(MODE, LAYOUT_ID,
                DefinitionId.of("zartra", "map/other"), DefinitionId.of("zartra", "tag/missing"),
                "not-found", 1, 0, 99, 1, SelectorFramework.Order.CONFIGURED);
        final SelectorFramework.Page empty = service.page(Collections.singleton(ready), mapFilter, 1);
        assertEquals(SelectorFramework.Status.EMPTY, empty.status());
        assertEquals(0, empty.page());
        assertFalse(service.quickJoin(Collections.<SelectorFramework.Candidate>emptyList(), mapFilter, 1).isPresent());
        assertThrows(IllegalArgumentException.class, () -> service.page(Collections.singleton(ready), mapFilter, -1));
        assertThrows(IllegalArgumentException.class, () -> service.select(empty, ready.arenaId()));
        assertThrows(IllegalArgumentException.class, () -> new SelectorFramework.Selection(ready.arenaId(), -1, 0));
    }

    @Test void queueValidationCompletionAndCapacityBranches() {
        assertThrows(AssertionError.class, () -> construct(MatchmakingFramework.class));
        assertEquals(MatchmakingFramework.QueueId.of("zartra", "q"),
                MatchmakingFramework.QueueId.parse("zartra:queue/q"));
        assertFalse(MatchmakingFramework.QueueId.of("zartra", "q").equals("q"));
        assertFalse(MatchmakingFramework.PartyId.of(new UUID(0, 1)).equals("p"));
        final MatchmakingFramework.PartyId partyId = MatchmakingFramework.PartyId.of(new UUID(0, 1));
        assertEquals(partyId, partyId);
        assertEquals(partyId, MatchmakingFramework.PartyId.parse(partyId.toString()));
        assertFalse(partyId.equals(MatchmakingFramework.PartyId.of(new UUID(0, 2))));
        final MatchmakingFramework.ReservationId reservationId =
                MatchmakingFramework.ReservationId.of(new UUID(0, 3));
        assertEquals(reservationId, reservationId);
        assertEquals(reservationId, MatchmakingFramework.ReservationId.of(new UUID(0, 3)));
        assertFalse(reservationId.equals(MatchmakingFramework.ReservationId.of(new UUID(0, 4))));
        assertFalse(reservationId.equals("reservation"));
        assertThrows(IllegalArgumentException.class, () -> new MatchmakingFramework.Party(
                MatchmakingFramework.PartyId.of(new UUID(0, 1)), player(1), Collections.<PlayerId>emptyList(), 0));
        assertThrows(IllegalArgumentException.class, () -> new MatchmakingFramework.Party(
                MatchmakingFramework.PartyId.of(new UUID(0, 1)), player(1), Arrays.asList(player(1), player(1)), 0));
        assertThrows(IllegalArgumentException.class, () -> new MatchmakingFramework.Party(
                MatchmakingFramework.PartyId.of(new UUID(0, 1)), player(1), Collections.singleton(player(2)), 0));
        assertThrows(IllegalArgumentException.class, () -> new MatchmakingFramework.Party(
                MatchmakingFramework.PartyId.of(new UUID(0, 1)), player(1), Collections.singleton(player(1)), -1));
        invalidLimits(1025, 1, 1);
        invalidLimits(1, 0, 1);
        invalidLimits(1, 100001, 1);
        invalidLimits(1, 1, 0);
        invalidLimits(1, 1, 1000001);
        assertThrows(IllegalArgumentException.class, () -> new MatchmakingFramework.Limits(1, 1, 1,
                Duration.ZERO, Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class, () -> new MatchmakingFramework.Limits(1, 1, 1,
                Duration.ofSeconds(1), Duration.ofSeconds(-1)));
        final MatchmakingFramework.QueueService service = queueService(1, 1, 2);
        final MatchmakingFramework.Request first = request(1, MatchmakingFramework.QueueId.of("zartra", "one"), 0, NOW.plusSeconds(30));
        assertEquals(MatchmakingFramework.EnqueueVerdict.ACCEPTED, service.enqueue(first).verdict());
        assertEquals(MatchmakingFramework.EnqueueVerdict.STALE,
                service.enqueue(request(1, first.queueId(), 1, NOW.plusSeconds(30))).verdict());
        assertEquals(MatchmakingFramework.EnqueueVerdict.CAPACITY,
                service.enqueue(request(2, MatchmakingFramework.QueueId.of("zartra", "two"), 0, NOW.plusSeconds(30))).verdict());
        assertFalse(service.complete(Collections.singleton(IdempotencyKey.of("m10", "unknown"))));
        assertTrue(service.complete(Collections.singleton(first.idempotencyKey())));
        assertFalse(service.status(first.actor()).isPresent());
        assertFalse(service.cancel(first.actor(), first.cancellationToken(), first.revision()));
        assertTrue(service.snapshot(first.queueId()).isEmpty());
        assertEquals(0, service.cleanup());
    }

    @Test void arenaAndReservationFailClosedBranches() {
        final MatchmakingFramework.Request request = request(1, MatchmakingFramework.QueueId.of("zartra", "one"), 0, NOW.plusSeconds(30));
        invalidArena(-1, 1, 2, 0);
        invalidArena(0, 0, 2, 0);
        invalidArena(0, 65, 65, 0);
        invalidArena(0, 1, 1, 0);
        invalidArena(0, 1, 257, 0);
        invalidArena(0, 1, 2, -1);
        invalidArena(0, 1, 2, 3);
        assertFalse(arena(true, true, true, true, false, 1, 8, 0,
                ModeFramework.ModeId.of("zartra", "other"), LAYOUT_ID).accepts(request, 0));
        assertFalse(arena(true, true, true, true, false, 1, 8, 0, MODE,
                DefinitionId.of("zartra", "layout/other")).accepts(request, 0));
        assertFalse(arena(true, true, true, true, false, 1, 2, 2, MODE, LAYOUT_ID).accepts(request, 0));
        final MatchmakingFramework.ReservationService bounded = new MatchmakingFramework.ReservationService(
                1, Duration.ofSeconds(30), TimeSource.FixedTimeSource.at(NOW));
        final MatchmakingFramework.ArenaAvailability available = arena(true, true, true, true, false, 1, 8, 0, MODE, LAYOUT_ID);
        final MatchmakingFramework.Reservation reservation = bounded.acquire(request, available).get();
        assertFalse(bounded.acquire(request(2, request.queueId(), 0, NOW.plusSeconds(30)), available).isPresent());
        assertThrows(IllegalArgumentException.class, () -> bounded.confirm(
                MatchmakingFramework.ReservationId.of(new UUID(8, 8)), 1));
        assertTrue(bounded.release(reservation.id()));
        assertEquals(1, bounded.cleanup());
        assertEquals(0, bounded.active());
        assertFalse(bounded.release(reservation.id()));
    }

    @Test void teamSelectorAndCompassTerminalBranches() {
        assertThrows(AssertionError.class, () -> construct(M10AddonSelection.class));
        final TeamDefinition team = TeamDefinition.of(DefinitionId.of("team", "red"), "Red",
                DefinitionId.of("color", "red"), 2);
        assertThrows(IllegalArgumentException.class, () -> new M10AddonSelection.TeamOption(team, -1, true, true));
        assertThrows(IllegalArgumentException.class, () -> new M10AddonSelection.TeamOption(team, 3, true, true));
        final M10AddonSelection.TeamSelector selector = new M10AddonSelection.TeamSelector(1);
        final M10AddonSelection.TeamOption allowed = new M10AddonSelection.TeamOption(team, 0, true, true);
        assertEquals(M10AddonSelection.TeamVerdict.FORBIDDEN,
                selector.select(arenaId(1), 1, player(1), null,
                        new M10AddonSelection.TeamOption(team, 0, false, true), false));
        assertEquals(M10AddonSelection.TeamVerdict.FORBIDDEN,
                selector.select(arenaId(1), 1, player(1), null,
                        new M10AddonSelection.TeamOption(team, 0, true, false), false));
        selector.select(arenaId(1), 1, player(1), null, allowed, false);
        assertThrows(IllegalStateException.class, () -> selector.select(arenaId(1), 1, player(2), null, allowed, false));
        assertTrue(selector.reconnect(player(1), 1, 0).isPresent());
        assertEquals(0, selector.reset(arenaId(2)));
        assertEquals(1, selector.reset(arenaId(1)));
        assertThrows(IllegalStateException.class, () -> selector.autoAssign(Collections.singleton(
                new M10AddonSelection.TeamOption(team, 2, true, true)), 1));
        assertThrows(IllegalArgumentException.class, () -> new M10AddonSelection.TrackerTarget(
                player(1), DefinitionId.of("team", "blue"), Double.NaN, true, true, false, false));
        assertThrows(IllegalArgumentException.class, () -> new M10AddonSelection.TrackerTarget(
                player(1), DefinitionId.of("team", "blue"), -1, true, true, false, false));
        final AtomicReference<Instant> now = new AtomicReference<Instant>(NOW);
        final M10AddonSelection.Compass compass = new M10AddonSelection.Compass(now::get,
                Duration.ofSeconds(1), 10, 1);
        assertFalse(compass.nearest(DefinitionId.of("team", "red"), Collections.singleton(
                tracker(1, "blue", 11, true, true, false, false))).isPresent());
        compass.callout(player(1), DefinitionId.of("team", "red"), M10AddonSelection.Callout.ATTACK, null);
        assertThrows(IllegalStateException.class, () -> compass.callout(player(2), DefinitionId.of("team", "red"),
                M10AddonSelection.Callout.ATTACK, null));
        now.set(NOW.plusSeconds(2));
        assertEquals(1, compass.cleanup());
    }

    @Test void spectatorStaleCleanupAndValidationBranches() {
        assertThrows(AssertionError.class, () -> construct(SpectatorFramework.class));
        assertThrows(IllegalArgumentException.class, () -> new SpectatorFramework.Preferences(-1, false, false, false));
        assertThrows(IllegalArgumentException.class, () -> new SpectatorFramework.Policy(100001,
                Duration.ofSeconds(1), true, restrictions()));
        assertThrows(IllegalArgumentException.class, () -> new SpectatorFramework.Policy(1,
                null, true, restrictions()));
        assertThrows(IllegalArgumentException.class, () -> new SpectatorFramework.Policy(1,
                Duration.ofSeconds(-1), true, restrictions()));
        final AtomicReference<Instant> now = new AtomicReference<Instant>(NOW);
        final SpectatorFramework.Service service = new SpectatorFramework.Service(
                new SpectatorFramework.Policy(4, Duration.ofSeconds(1), true, restrictions()),
                now::get, event -> { });
        assertFalse(service.session(player(1)).isPresent());
        assertThrows(IllegalArgumentException.class, () -> service.leave(player(1), 0));
        final SpectatorFramework.Session active = service.enter(match(1), SpectatorFramework.EntryReason.STAFF, state(1));
        assertThrows(IllegalStateException.class, () -> service.enter(match(2), SpectatorFramework.EntryReason.STAFF, state(1)));
        assertThrows(IllegalStateException.class, () -> service.preferences(player(1), 99,
                new SpectatorFramework.Preferences(5, false, true, false)));
        assertThrows(IllegalArgumentException.class, () -> service.target(player(1), active.revision(),
                target(1, match(1), true, true, false, true, 0)));
        assertThrows(IllegalStateException.class, () -> service.navigate(player(1), active.revision(),
                Collections.<SpectatorFramework.Target>emptyList(), true));
        assertEquals(active.id(), service.targetUnavailable(player(1), active.revision(), player(9)).id());
        final SpectatorFramework.Session disconnected = service.disconnect(player(1), active.revision());
        now.set(NOW.plusSeconds(2));
        final List<SpectatorFramework.Restoration> expired = service.cleanup();
        assertEquals(1, expired.size());
        assertThrows(IllegalStateException.class, () -> service.reconnect(player(1), disconnected.revision(),
                Collections.<SpectatorFramework.Target>emptyList()));
        final SpectatorFramework.Restoration restoration = expired.get(0);
        assertFalse(service.restored(player(1), restoration.sessionId(), restoration.sessionRevision() + 1));
        assertTrue(service.restored(player(1), restoration.sessionId(), restoration.sessionRevision()));
        assertTrue(service.cleanupCount() > 0);
        assertTrue(service.staleRejected() > 0);
    }

    private static void invalidQuery(final String search, final int capacity, final long revision,
                                     final int page, final int size) {
        assertThrows(IllegalArgumentException.class, () -> new SelectorFramework.Query(null, null,
                null, null, search, capacity, revision, page, size, SelectorFramework.Order.IDENTITY));
    }
    private static void invalidLimits(final int queues, final int requests, final int actors) {
        assertThrows(IllegalArgumentException.class, () -> new MatchmakingFramework.Limits(queues,
                requests, actors, Duration.ofSeconds(1), Duration.ofSeconds(1)));
    }
    private static void invalidArena(final long revision, final int teamCapacity,
                                     final int totalCapacity, final int occupied) {
        assertThrows(IllegalArgumentException.class, () -> arena(true, true, true, true, false,
                teamCapacity, totalCapacity, occupied, MODE, LAYOUT_ID, revision));
    }
    private static MatchmakingFramework.QueueService queueService(final int queues, final int requests,
                                                                  final int actors) {
        return new MatchmakingFramework.QueueService(new MatchmakingFramework.Limits(queues, requests,
                actors, Duration.ofMinutes(1), Duration.ofSeconds(1)), TimeSource.FixedTimeSource.at(NOW));
    }
    private static MatchmakingFramework.Request request(final int seed,
            final MatchmakingFramework.QueueId queue, final long revision, final Instant deadline) {
        return new MatchmakingFramework.Request(queue, player(seed), null, MODE, LAYOUT_ID, 1,
                Collections.<ArenaId>emptySet(), Collections.<DefinitionId>emptyList(), "en_GB", "local",
                0, NOW, revision, new UUID(seed, 9), deadline, IdempotencyKey.of("m10", "branch-" + seed),
                CorrelationId.of(new UUID(seed, 10)));
    }
    private static MatchmakingFramework.ArenaAvailability arena(final boolean enabled,
            final boolean healthy, final boolean worldReady, final boolean joinable,
            final boolean recovering, final int teamCapacity, final int totalCapacity,
            final int occupied, final ModeFramework.ModeId mode, final DefinitionId layout) {
        return arena(enabled, healthy, worldReady, joinable, recovering, teamCapacity, totalCapacity,
                occupied, mode, layout, 1);
    }
    private static MatchmakingFramework.ArenaAvailability arena(final boolean enabled,
            final boolean healthy, final boolean worldReady, final boolean joinable,
            final boolean recovering, final int teamCapacity, final int totalCapacity,
            final int occupied, final ModeFramework.ModeId mode, final DefinitionId layout,
            final long revision) {
        return new MatchmakingFramework.ArenaAvailability(arenaId(1), revision, mode, layout,
                teamCapacity, totalCapacity, occupied, enabled, healthy, worldReady, joinable, recovering);
    }
    private static ModeFramework.Definition definition(final boolean enabled, final int minTeams,
            final int maxTeams, final int minPlayers, final int maxPlayers,
            final List<DefinitionId> capabilities, final List<ModeFramework.ConfigField> schema,
            final List<ModeFramework.DeferredBinding> deferred) {
        return new ModeFramework.Definition(MODE, MessageKey.of("mode.name"),
                MessageKey.of("mode.description"), new ModeFramework.Version(1, 0), enabled,
                minTeams, maxTeams, minPlayers, maxPlayers, capabilities, schema, deferred);
    }
    private static ModeFramework.Layout layout(final int count, final int capacity) {
        final List<DefinitionId> teams = new ArrayList<DefinitionId>();
        for (int index = 0; index < count; index++) { teams.add(team(index)); }
        return new ModeFramework.Layout(LAYOUT_ID, teams, capacity);
    }
    private static DefinitionId team(final int index) { return DefinitionId.of("team", "semantic-" + index); }
    private static SelectorFramework.Candidate candidate(final int seed, final ModeFramework.Layout layout,
            final boolean enabled, final boolean healthy, final boolean world, final SelectorFramework.Lifecycle lifecycle,
            final int players, final int reserved, final long revision) {
        return new SelectorFramework.Candidate(arenaId(seed), revision,
                DefinitionId.of("zartra", "map/map-" + seed), MODE, layout, MessageKey.of("arena.name"),
                enabled, healthy, world, lifecycle, players, reserved, seed,
                Collections.singleton(DefinitionId.of("zartra", "tag/all")));
    }
    private static SpectatorFramework.Restrictions restrictions() {
        return new SpectatorFramework.Restrictions(true, true, false,
                Collections.singleton(DefinitionId.of("zartra", "command/leave")));
    }
    private static PlayerStateSnapshot state(final int seed) {
        return new PlayerStateSnapshot(player(seed), PlayerStateSnapshot.Inventory.empty(36),
                new PlayerStateSnapshot.Location(DefinitionId.of("zartra", "world/lobby"), 0, 70, 0, 0, 0),
                PlayerStateSnapshot.Mode.ADVENTURE, true);
    }
    private static SpectatorFramework.Target target(final int seed, final MatchId match,
            final boolean living, final boolean visible, final boolean vanished, final boolean consent,
            final int order) {
        return new SpectatorFramework.Target(player(seed), match, team(seed), living, visible,
                vanished, consent, order);
    }
    private static M10AddonSelection.TrackerTarget tracker(final int seed, final String team,
            final double distance, final boolean living, final boolean visible,
            final boolean vanished, final boolean spectator) {
        return new M10AddonSelection.TrackerTarget(player(seed), DefinitionId.of("team", team),
                distance, living, visible, vanished, spectator);
    }
    private static PlayerId player(final int seed) { return PlayerId.of(new UUID(0, seed)); }
    private static ArenaId arenaId(final int seed) { return ArenaId.of(new UUID(1, seed)); }
    private static MatchId match(final int seed) { return MatchId.of(new UUID(2, seed)); }
    private static String repeat(final char value, final int count) {
        final StringBuilder result = new StringBuilder(count);
        for (int index = 0; index < count; index++) { result.append(value); }
        return result.toString();
    }
    private static void construct(final Class<?> type) {
        try {
            final java.lang.reflect.Constructor<?> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        } catch (java.lang.reflect.InvocationTargetException failure) {
            if (failure.getCause() instanceof Error) { throw (Error) failure.getCause(); }
            throw new IllegalStateException(failure.getCause());
        } catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
    }
}
