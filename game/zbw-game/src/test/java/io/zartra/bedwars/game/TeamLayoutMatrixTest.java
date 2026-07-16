package io.zartra.bedwars.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.ArenaId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.GeneratorTypeId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.MapId;
import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.identity.ResourceId;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.arena.model.ArenaBundle;
import io.zartra.bedwars.arena.model.ArenaDefinition;
import io.zartra.bedwars.arena.model.ArenaGenerator;
import io.zartra.bedwars.arena.model.ArenaLocation;
import io.zartra.bedwars.arena.model.ArenaNpc;
import io.zartra.bedwars.arena.model.ArenaRegion;
import io.zartra.bedwars.arena.model.ArenaTeam;
import io.zartra.bedwars.arena.model.MapDefinition;
import io.zartra.bedwars.arena.validation.ArenaValidation;
import io.zartra.bedwars.game.application.ArenaMatchAssembler;
import io.zartra.bedwars.game.application.MatchAssemblyRequest;
import io.zartra.bedwars.game.model.MatchCommand;
import io.zartra.bedwars.game.model.MatchSnapshot;
import io.zartra.bedwars.game.model.MatchStateMachine;
import io.zartra.bedwars.game.model.MatchTimingPolicy;
import io.zartra.bedwars.game.model.MatchTransition;
import io.zartra.bedwars.game.model.PlayerStateSnapshot;
import io.zartra.bedwars.game.model.StandardVictoryEvaluator;
import io.zartra.bedwars.game.model.TeamAssignmentPolicy;
import io.zartra.bedwars.game.model.TeamDefinition;
import io.zartra.bedwars.game.model.TeamSnapshot;
import io.zartra.bedwars.game.model.VictoryEvaluation;
import io.zartra.bedwars.world.api.WorldKey;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TeamLayoutMatrixTest {
    private static final Instant NOW = Instant.parse("2026-07-16T00:00:00Z");
    private static final ArenaId ARENA_ID = ArenaId.of(
            UUID.fromString("81000000-0000-0000-0000-000000000001"));
    private static final MapId MAP_ID = MapId.of(
            UUID.fromString("82000000-0000-0000-0000-000000000001"));
    private static final MatchId MATCH_ID = MatchId.of(
            UUID.fromString("83000000-0000-0000-0000-000000000001"));
    private static final long ARENA_VERSION = 7L;
    private static final long MAP_VERSION = 11L;
    private static final String[] STANDARD_NAMES = {
        "Red", "Blue", "Green", "Yellow", "Aqua", "White", "Pink", "Gray"
    };

    @Test void assemblesSoloEightByOne() { assertLayout(8, 1); }
    @Test void assemblesDoublesEightByTwo() { assertLayout(8, 2); }
    @Test void assemblesThreeByThreeByThreeByThree() { assertLayout(4, 3); }
    @Test void assemblesFourByFourByFourByFour() { assertLayout(4, 4); }
    @Test void assemblesFourByFour() { assertLayout(2, 4); }
    @Test void assemblesCustomTwelveByThreeWithoutPresetCeiling() { assertLayout(12, 3); }
    @Test void assemblesSharedMaximumSixtyFourByFour() { assertLayout(64, 4); }

    @Test void assembledLifecycleReconnectVictoryResetAndRecoveryRemainGeneric() {
        final MatchStateMachine machine = assemble(layout(2, 4, ArenaDefinition.Status.ENABLED));
        final DefinitionId first = id("team/red");
        final DefinitionId second = id("team/blue");
        final PlayerId one = player(1);
        final PlayerId two = player(2);
        success(machine.apply(MatchCommand.admit(first, state(one)), NOW));
        success(machine.apply(MatchCommand.admit(second, state(two)), NOW));
        success(machine.apply(MatchCommand.forceStart(), NOW.plusSeconds(1)));
        success(machine.apply(MatchCommand.disconnect(one), NOW.plusSeconds(2)));
        success(machine.apply(MatchCommand.reconnect(one), NOW.plusSeconds(3)));
        success(machine.apply(MatchCommand.destroyBed(second), NOW.plusSeconds(4)));
        final MatchTransition elimination = success(machine.apply(
                MatchCommand.eliminate(two), NOW.plusSeconds(5)));
        assertTrue(elimination.completionIntent().isPresent());
        final VictoryEvaluation.CompletionIntent intent = elimination.completionIntent().get();
        assertEquals(first, intent.winningTeamId());
        final MatchTransition retry = success(machine.apply(
                MatchCommand.eliminate(two), NOW.plusSeconds(5)));
        assertTrue(retry.duplicate());
        assertEquals(intent, retry.completionIntent().get());
        final IdempotencyKey key = IdempotencyKey.of("zartra", "m08_1/layout_matrix");
        success(machine.apply(intent.completionCommand(key), NOW.plusSeconds(6)));
        success(machine.apply(MatchCommand.commitCompletion(key), NOW.plusSeconds(7)));
        success(machine.apply(MatchCommand.restore(one), NOW.plusSeconds(8)));
        success(machine.apply(MatchCommand.restore(two), NOW.plusSeconds(9)));
        success(machine.apply(MatchCommand.finishReset(), NOW.plusSeconds(10)));
        assertEquals(MatchSnapshot.State.WAITING, machine.snapshot().state());
        assertEquals("Red", machine.snapshot().team(first).get().displayName());
        assertEquals(id("color/red"), machine.snapshot().team(first).get().color());
        final MatchStateMachine recovered = MatchStateMachine.recover(machine.snapshot(),
                machine.rules(), machine.victoryEvaluator());
        assertEquals(machine.snapshot(), recovered.snapshot());
        assertEquals(machine.victoryEvaluator(), recovered.victoryEvaluator());
    }

    @Test void assemblerRejectsDisabledStaleAndInconsistentDefinitions() {
        final ArenaBundle enabled = layout(2, 4, ArenaDefinition.Status.ENABLED);
        assertFailure(new MatchAssemblyRequest(MATCH_ID,
                layout(2, 4, ArenaDefinition.Status.DISABLED), ARENA_VERSION, MAP_VERSION,
                timing(), NOW), "zartra:game/assembly/arena_unavailable");
        assertFailure(new MatchAssemblyRequest(MATCH_ID, enabled, ARENA_VERSION + 1,
                MAP_VERSION, timing(), NOW), "zartra:game/assembly/stale_definition");
        assertFailure(new MatchAssemblyRequest(MATCH_ID, enabled, ARENA_VERSION,
                MAP_VERSION + 1, timing(), NOW), "zartra:game/assembly/stale_definition");
        final ArenaBundle insufficient = new ArenaBundle(enabled.arena().toBuilder()
                .playerLimits(2, 9, 4).build(), enabled.map());
        assertFailure(request(insufficient), "zartra:game/assembly/invalid_definition");
        final ArenaBundle wrongGroup = new ArenaBundle(enabled.arena().toBuilder()
                .group(id("group/other")).build(), enabled.map());
        assertFailure(request(wrongGroup), "zartra:game/assembly/invalid_definition");
        final ArenaBundle wrongMode = new ArenaBundle(enabled.arena().toBuilder()
                .modes(Collections.singleton(id("mode/custom"))).build(), enabled.map());
        assertFailure(request(wrongMode), "zartra:game/assembly/invalid_definition");
        assertThrows(IllegalArgumentException.class, () -> new MatchAssemblyRequest(
                MATCH_ID, enabled, -1, MAP_VERSION, timing(), NOW));
        assertThrows(NullPointerException.class,
                () -> new ArenaMatchAssembler().assemble(null));
    }

    @Test void victoryContractIsGenericAndOverrideableWithoutPaperOrModes() {
        final MatchStateMachine machine = new ArenaMatchAssembler(
                new ArenaValidation.DefaultValidator(), snapshot -> VictoryEvaluation.none())
                .assemble(request(layout(2, 1, ArenaDefinition.Status.ENABLED))).requireValue();
        final PlayerId one = player(1);
        final PlayerId two = player(2);
        success(machine.apply(MatchCommand.admit(id("team/red"), state(one)), NOW));
        success(machine.apply(MatchCommand.admit(id("team/blue"), state(two)), NOW));
        success(machine.apply(MatchCommand.forceStart(), NOW));
        final MatchTransition transition = success(machine.apply(
                MatchCommand.eliminate(two), NOW.plusSeconds(1)));
        assertFalse(transition.completionIntent().isPresent());
        final StandardVictoryEvaluator standard = new StandardVictoryEvaluator();
        assertFalse(standard.evaluate(assemble(
                layout(2, 1, ArenaDefinition.Status.ENABLED)).snapshot())
                .completionRequired());
    }

    @Test void teamAndTimingValuesAreImmutableValidatedAndComparable() {
        final TeamDefinition definition = TeamDefinition.of(id("team/original"), "Original",
                id("color/original"), 3);
        assertEquals(definition, TeamDefinition.of(id("team/original"), "Original",
                id("color/original"), 3));
        assertEquals(definition.hashCode(), TeamDefinition.of(id("team/original"), "Original",
                id("color/original"), 3).hashCode());
        assertNotEquals(definition, TeamDefinition.of(id("team/other"), "Other",
                id("color/other"), 3));
        final TeamSnapshot snapshot = TeamSnapshot.empty(definition);
        assertEquals(definition, snapshot.definition());
        assertEquals("Original", snapshot.displayName());
        assertEquals(id("color/original"), snapshot.color());
        assertThrows(IllegalArgumentException.class, () -> TeamDefinition.of(
                id("team/bad"), "", id("color/bad"), 1));
        assertThrows(IllegalArgumentException.class, () -> TeamDefinition.of(
                id("team/bad"), "Bad", id("color/bad"), 65));
        assertEquals(timing(), timing());
        assertEquals(timing().hashCode(), timing().hashCode());
        assertThrows(IllegalArgumentException.class, () -> new MatchTimingPolicy(
                0, Duration.ofSeconds(1), Duration.ofSeconds(1), Duration.ofSeconds(1)));
    }

    private static void assertLayout(final int teamCount, final int teamSize) {
        final ArenaBundle bundle = layout(teamCount, teamSize, ArenaDefinition.Status.ENABLED);
        final MatchAssemblyRequest request = request(bundle);
        assertEquals(MATCH_ID, request.matchId());
        assertEquals(bundle, request.arenaBundle());
        assertEquals(ARENA_VERSION, request.expectedArenaVersion());
        assertEquals(MAP_VERSION, request.expectedMapVersion());
        assertEquals(timing(), request.timingPolicy());
        assertEquals(NOW, request.createdAt());
        final MatchStateMachine machine = assemble(bundle);
        assertEquals(MatchSnapshot.State.WAITING, machine.snapshot().state());
        assertEquals(ARENA_ID, machine.snapshot().arenaId());
        assertEquals(teamCount, machine.snapshot().teams().size());
        assertEquals(teamCount * teamSize, machine.rules().maximumPlayers());
        final TeamAssignmentPolicy assignments = new TeamAssignmentPolicy();
        int playerNumber = 1;
        for (ArenaTeam configured : bundle.arena().teams()) {
            final TeamSnapshot runtime = machine.snapshot().team(configured.id()).get();
            assertEquals(configured.displayName(), runtime.displayName());
            assertEquals(configured.color(), runtime.color());
            assertEquals(teamSize, runtime.capacity());
            final PlayerId player = player(playerNumber++);
            assertEquals(configured.id(), assignments.assign(player, machine.snapshot(),
                    Optional.of(configured.id())).requireValue());
            success(machine.apply(MatchCommand.admit(configured.id(), state(player)), NOW));
        }
        if (teamCount == 8) {
            for (String name : STANDARD_NAMES) {
                final String path = name.toLowerCase(java.util.Locale.ROOT);
                assertEquals(name, machine.snapshot().team(id("team/" + path)).get()
                        .displayName());
                assertEquals(id("color/" + path), machine.snapshot()
                        .team(id("team/" + path)).get().color());
            }
        }
    }

    private static MatchStateMachine assemble(final ArenaBundle bundle) {
        final Result<MatchStateMachine> result = new ArenaMatchAssembler().assemble(request(bundle));
        assertTrue(result.isSuccess(), () -> String.valueOf(result.error()));
        return result.requireValue();
    }

    private static MatchAssemblyRequest request(final ArenaBundle bundle) {
        return new MatchAssemblyRequest(MATCH_ID, bundle, ARENA_VERSION, MAP_VERSION,
                timing(), NOW);
    }

    private static void assertFailure(final MatchAssemblyRequest request,
                                      final String expectedCode) {
        final Result<MatchStateMachine> result = new ArenaMatchAssembler().assemble(request);
        assertTrue(result.isFailure());
        assertEquals(expectedCode, result.error().get().code().toString());
    }

    private static MatchTimingPolicy timing() {
        return new MatchTimingPolicy(2, Duration.ofSeconds(30), Duration.ofSeconds(5),
                Duration.ofSeconds(1));
    }

    private static MatchTransition success(final Result<MatchTransition> result) {
        assertTrue(result.isSuccess(), () -> String.valueOf(result.error()));
        return result.requireValue();
    }

    private static ArenaBundle layout(final int teamCount, final int teamSize,
                                      final ArenaDefinition.Status status) {
        final List<ArenaTeam> teams = new ArrayList<ArenaTeam>();
        final List<ArenaGenerator> generators = new ArrayList<ArenaGenerator>();
        final List<ArenaNpc> npcs = new ArrayList<ArenaNpc>();
        for (int index = 0; index < teamCount; index++) {
            final String name = index < STANDARD_NAMES.length
                    ? STANDARD_NAMES[index] : "Custom " + (index + 1);
            final String path = index < STANDARD_NAMES.length
                    ? name.toLowerCase(java.util.Locale.ROOT) : "custom_" + (index + 1);
            final DefinitionId teamId = id("team/" + path);
            final double x = 10.0D + index * 10.0D;
            teams.add(ArenaTeam.create(teamId, name, id("color/" + path))
                    .withSpawn(location(x, 10, 10))
                    .withBed(location(x, 10, 12), id("facing/north")));
            generators.add(generator("generator/" + path, "team_iron", "iron", teamId,
                    x, 10, 14));
            npcs.add(ArenaNpc.of(id("npc/" + path + "/shop"), ArenaNpc.Kind.SHOP,
                    teamId, location(x, 10, 16)));
            npcs.add(ArenaNpc.of(id("npc/" + path + "/upgrade"),
                    ArenaNpc.Kind.TEAM_UPGRADE, teamId, location(x, 10, 18)));
        }
        generators.add(generator("generator/diamond", "diamond", "diamond", null,
                800, 10, 100));
        generators.add(generator("generator/emerald", "emerald", "emerald", null,
                800, 10, 110));
        final Set<DefinitionId> modes = Collections.singleton(id("mode/standard"));
        final ArenaDefinition arena = ArenaDefinition.builder(ARENA_ID, MAP_ID,
                "Layout Matrix", NOW).worlds(WorldKey.of("layout_active"),
                WorldKey.of("layout_template")).group(id("group/layout")).modes(modes)
                .playerLimits(2, teamCount * teamSize, teamSize)
                .waitingSpawn(location(500, 20, 500)).spectatorSpawn(location(500, 30, 500))
                .bounds(ArenaRegion.between(id("region/bounds"), location(0, 0, 0),
                        location(1000, 100, 1000))).limits(-1, 0, 100)
                .teams(teams).generators(generators).npcs(npcs).status(status)
                .revision(ARENA_VERSION, NOW).build();
        final MapDefinition map = new MapDefinition(MAP_ID, "Layout Matrix", NOW, NOW,
                MAP_VERSION, id("template/layout"), id("group/layout"), id("author/test"),
                "Deterministic configurable layout", modes, 1, 64,
                new HashSet<DefinitionId>(), Collections.<DefinitionId, String>emptyMap(),
                id("validation/valid"));
        return new ArenaBundle(arena, map);
    }

    private static PlayerStateSnapshot state(final PlayerId playerId) {
        return new PlayerStateSnapshot(playerId, PlayerStateSnapshot.Inventory.empty(36),
                new PlayerStateSnapshot.Location(id("world/lobby"), 0, 64, 0, 0, 0),
                PlayerStateSnapshot.Mode.SURVIVAL, true);
    }

    private static PlayerId player(final int number) {
        return PlayerId.of(new UUID(0L, number));
    }

    private static DefinitionId id(final String path) {
        return DefinitionId.of("zartra", path);
    }

    private static ArenaLocation location(final double x, final double y, final double z) {
        return ArenaLocation.of(x, y, z, 0, 0);
    }

    private static ArenaGenerator generator(
            final String identity, final String type, final String resource,
            final DefinitionId teamId, final double x, final double y, final double z) {
        return ArenaGenerator.of(id(identity), GeneratorTypeId.of("zartra", type),
                ResourceId.of("zartra", resource), teamId, location(x, y, z),
                Duration.ofSeconds(1));
    }
}
