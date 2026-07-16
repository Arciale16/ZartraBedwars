package io.zartra.bedwars.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.game.application.GameEngineService;
import io.zartra.bedwars.game.model.GameRules;
import io.zartra.bedwars.game.model.MatchCommand;
import io.zartra.bedwars.game.model.MatchSnapshot;
import io.zartra.bedwars.game.model.MatchStateMachine;
import io.zartra.bedwars.game.model.PlayerSession;
import io.zartra.bedwars.game.model.TeamAssignmentPolicy;
import io.zartra.bedwars.game.model.TeamSnapshot;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GameBranchCoverageTest {
    private static final DefinitionId OUTCOME = DefinitionId.of("zartra", "outcome/test");
    private static final IdempotencyKey KEY = IdempotencyKey.of("zartra", "completion/test");

    @Test void matchConstructionSnapshotAndRecoveryRejectEveryMalformedShape() {
        assertThrows(NullPointerException.class, () -> new MatchStateMachine(GameFixtures.match(1),
                GameFixtures.arena(1), GameFixtures.rules(), null, GameFixtures.NOW));
        assertThrows(IllegalArgumentException.class, () -> new MatchStateMachine(GameFixtures.match(1),
                GameFixtures.arena(1), GameFixtures.rules(), Collections.singletonList(
                TeamSnapshot.empty(GameFixtures.BLUE, 4)), GameFixtures.NOW));
        assertThrows(IllegalArgumentException.class, () -> new MatchStateMachine(GameFixtures.match(1),
                GameFixtures.arena(1), GameFixtures.rules(), Arrays.asList(
                TeamSnapshot.empty(GameFixtures.BLUE, 2), TeamSnapshot.empty(GameFixtures.BLUE, 2)), GameFixtures.NOW));
        assertThrows(IllegalArgumentException.class, () -> new MatchStateMachine(GameFixtures.match(1),
                GameFixtures.arena(1), GameFixtures.rules(), Arrays.asList(
                TeamSnapshot.empty(GameFixtures.BLUE, 1), TeamSnapshot.empty(GameFixtures.RED, 1)), GameFixtures.NOW));
        assertThrows(IllegalArgumentException.class, () -> new MatchSnapshot(GameFixtures.match(1),
                GameFixtures.arena(1), -1, MatchSnapshot.State.WAITING, 0, teams(),
                Collections.<PlayerSession>emptyList(), null, null, false, GameFixtures.NOW));
        assertThrows(IllegalArgumentException.class, () -> new MatchSnapshot(GameFixtures.match(1),
                GameFixtures.arena(1), 0, MatchSnapshot.State.WAITING, -1, teams(),
                Collections.<PlayerSession>emptyList(), null, null, false, GameFixtures.NOW));
        assertThrows(IllegalArgumentException.class, () -> new MatchSnapshot(GameFixtures.match(1),
                GameFixtures.arena(1), 0, MatchSnapshot.State.WAITING, 0,
                Collections.singletonList(TeamSnapshot.empty(GameFixtures.BLUE, 4)),
                Collections.<PlayerSession>emptyList(), null, null, false, GameFixtures.NOW));
        assertThrows(IllegalArgumentException.class, () -> new MatchSnapshot(GameFixtures.match(1),
                GameFixtures.arena(1), 0, MatchSnapshot.State.WAITING, 0,
                Arrays.asList(TeamSnapshot.empty(GameFixtures.BLUE, 2), TeamSnapshot.empty(GameFixtures.BLUE, 2)),
                Collections.<PlayerSession>emptyList(), null, null, false, GameFixtures.NOW));
        final PlayerSession unknownTeam = PlayerSession.waiting(DefinitionId.of("zartra", "team/unknown"),
                GameFixtures.state(GameFixtures.player(1)));
        assertThrows(IllegalArgumentException.class, () -> snapshot(MatchSnapshot.State.WAITING,
                Collections.singletonList(unknownTeam), teams(), null, null, false));
        final PlayerSession duplicateOne = PlayerSession.waiting(GameFixtures.BLUE, GameFixtures.state(GameFixtures.player(1)));
        final PlayerSession duplicateTwo = PlayerSession.waiting(GameFixtures.RED, GameFixtures.state(GameFixtures.player(1)));
        assertThrows(IllegalArgumentException.class, () -> snapshot(MatchSnapshot.State.WAITING,
                Arrays.asList(duplicateOne, duplicateTwo), teams(), null, null, false));
        assertThrows(IllegalArgumentException.class, () -> snapshot(MatchSnapshot.State.COMPLETING,
                Collections.<PlayerSession>emptyList(), teams(), OUTCOME, null, false));
        assertThrows(IllegalArgumentException.class, () -> snapshot(MatchSnapshot.State.COMPLETING,
                Collections.<PlayerSession>emptyList(), teams(), null, KEY, false));
        assertThrows(IllegalArgumentException.class, () -> snapshot(MatchSnapshot.State.RESETTING,
                Collections.<PlayerSession>emptyList(), teams(), null, null, true));
        assertEquals(0, GameFixtures.machine().snapshot().activeSessionCount());
        assertFalse(GameFixtures.machine().snapshot().team(DefinitionId.of("zartra", "team/x")).isPresent());
    }

    @Test void admissionRemovalCountdownStartAndCapacityFailuresAreCovered() {
        final MatchStateMachine machine = GameFixtures.machine();
        final PlayerId one = GameFixtures.player(1);
        final PlayerId two = GameFixtures.player(2);
        final PlayerId three = GameFixtures.player(3);
        assertTrue(machine.apply(MatchCommand.admit(DefinitionId.of("zartra", "team/missing"),
                GameFixtures.state(one)), GameFixtures.NOW).isFailure());
        apply(machine, MatchCommand.admit(GameFixtures.BLUE, GameFixtures.state(one)));
        apply(machine, MatchCommand.admit(GameFixtures.BLUE, GameFixtures.state(two)));
        assertTrue(machine.apply(MatchCommand.admit(GameFixtures.BLUE, GameFixtures.state(three)),
                GameFixtures.NOW).isFailure());
        assertTrue(machine.apply(MatchCommand.cancelCountdown(), GameFixtures.NOW).isFailure());
        assertTrue(machine.apply(MatchCommand.tick(), GameFixtures.NOW).isFailure());
        apply(machine, MatchCommand.startCountdown());
        assertTrue(machine.apply(MatchCommand.admit(GameFixtures.RED, GameFixtures.state(three)),
                GameFixtures.NOW).isSuccess());
        assertTrue(machine.apply(MatchCommand.remove(GameFixtures.player(9)), GameFixtures.NOW).isFailure());
        apply(machine, MatchCommand.cancelCountdown());
        apply(machine, MatchCommand.forceStart());
        assertTrue(machine.apply(MatchCommand.admit(GameFixtures.RED, GameFixtures.state(GameFixtures.player(4))),
                GameFixtures.NOW).isFailure());
        assertTrue(machine.apply(MatchCommand.remove(one), GameFixtures.NOW).isFailure());
        assertTrue(machine.apply(MatchCommand.startCountdown(), GameFixtures.NOW).isFailure());
        assertTrue(machine.apply(MatchCommand.forceStart(), GameFixtures.NOW).isFailure());
    }

    @Test void disconnectReconnectEliminationAndBedFailureBranchesAreCovered() {
        final PlayerId one = GameFixtures.player(1);
        final MatchStateMachine machine = GameFixtures.machine();
        apply(machine, MatchCommand.admit(GameFixtures.BLUE, GameFixtures.state(one)));
        assertTrue(machine.apply(MatchCommand.disconnect(GameFixtures.player(9)), GameFixtures.NOW).isFailure());
        apply(machine, MatchCommand.disconnect(one));
        assertTrue(machine.apply(MatchCommand.disconnect(one), GameFixtures.NOW).requireValue().duplicate());
        apply(machine, MatchCommand.reconnect(one));
        assertTrue(machine.apply(MatchCommand.reconnect(one), GameFixtures.NOW).requireValue().duplicate());
        apply(machine, MatchCommand.forceStart());
        assertTrue(machine.apply(MatchCommand.destroyBed(DefinitionId.of("zartra", "team/missing")),
                GameFixtures.NOW).isFailure());
        assertTrue(machine.apply(MatchCommand.eliminate(GameFixtures.player(9)), GameFixtures.NOW).isFailure());
        apply(machine, MatchCommand.eliminate(one));
        assertTrue(machine.apply(MatchCommand.eliminate(one), GameFixtures.NOW).requireValue().duplicate());
        assertTrue(machine.apply(MatchCommand.disconnect(one), GameFixtures.NOW).isSuccess());
        assertTrue(machine.apply(MatchCommand.disconnect(one), GameFixtures.NOW).requireValue().duplicate());
        assertTrue(machine.apply(MatchCommand.reconnect(GameFixtures.player(9)), GameFixtures.NOW).isFailure());
    }

    @Test void manualRecoveredStatesCoverDefensiveImpossibleTransitionBranches() {
        final PlayerId one = GameFixtures.player(1);
        final PlayerSession waiting = PlayerSession.waiting(GameFixtures.BLUE, GameFixtures.state(one));
        MatchStateMachine machine = MatchStateMachine.recover(snapshot(MatchSnapshot.State.PLAYING,
                Collections.singletonList(waiting), teamsWith(one), null, null, false), GameFixtures.rules());
        assertTrue(machine.apply(MatchCommand.eliminate(one), GameFixtures.NOW).isFailure());
        machine = MatchStateMachine.recover(snapshot(MatchSnapshot.State.RESETTING,
                Collections.singletonList(waiting), teamsWith(one), OUTCOME, KEY, true), GameFixtures.rules());
        assertTrue(machine.apply(MatchCommand.disconnect(one), GameFixtures.NOW).isFailure());
        assertTrue(machine.apply(MatchCommand.restore(one), GameFixtures.NOW).isFailure());
        assertTrue(machine.apply(MatchCommand.finishReset(), GameFixtures.NOW).isFailure());
        final PlayerSession disconnected = waiting.activate().disconnect(GameFixtures.NOW);
        machine = MatchStateMachine.recover(snapshot(MatchSnapshot.State.COUNTDOWN,
                Collections.singletonList(disconnected), teamsWith(one), null, null, false), GameFixtures.rules());
        apply(machine, MatchCommand.tick());
        assertEquals(MatchSnapshot.State.WAITING, machine.snapshot().state());
    }

    @Test void completionConflictWrongStateWrongKeyAndRestoreMissingAreCovered() {
        final PlayerId one = GameFixtures.player(1);
        final MatchStateMachine machine = GameFixtures.machine();
        assertTrue(machine.apply(MatchCommand.complete(OUTCOME, KEY), GameFixtures.NOW).isFailure());
        assertTrue(machine.apply(MatchCommand.commitCompletion(KEY), GameFixtures.NOW).isFailure());
        assertTrue(machine.apply(MatchCommand.restore(one), GameFixtures.NOW).isFailure());
        apply(machine, MatchCommand.admit(GameFixtures.BLUE, GameFixtures.state(one)));
        apply(machine, MatchCommand.forceStart());
        apply(machine, MatchCommand.complete(OUTCOME, KEY));
        assertTrue(machine.apply(MatchCommand.complete(DefinitionId.of("zartra", "outcome/other"), KEY),
                GameFixtures.NOW).isFailure());
        assertTrue(machine.apply(MatchCommand.commitCompletion(IdempotencyKey.of("zartra", "completion/other")),
                GameFixtures.NOW).isFailure());
        apply(machine, MatchCommand.commitCompletion(KEY));
        assertTrue(machine.apply(MatchCommand.commitCompletion(IdempotencyKey.of("zartra", "completion/other")),
                GameFixtures.NOW).isFailure());
        assertTrue(machine.apply(MatchCommand.restore(GameFixtures.player(9)), GameFixtures.NOW).isFailure());
    }

    @Test void teamAssignmentAndTeamConstructorCoverAllCapacityAndEqualityPaths() {
        assertThrows(IllegalArgumentException.class, () -> new TeamSnapshot(GameFixtures.BLUE, 0,
                Collections.<PlayerId>emptyList(), true, false));
        assertThrows(IllegalArgumentException.class, () -> new TeamSnapshot(GameFixtures.BLUE, 65,
                Collections.<PlayerId>emptyList(), true, false));
        assertThrows(IllegalArgumentException.class, () -> new TeamSnapshot(GameFixtures.BLUE, 2,
                Arrays.asList(GameFixtures.player(1), GameFixtures.player(1)), true, false));
        assertThrows(IllegalArgumentException.class, () -> new TeamSnapshot(GameFixtures.BLUE, 1,
                Arrays.asList(GameFixtures.player(1), GameFixtures.player(2)), true, false));
        final TeamSnapshot eliminated = TeamSnapshot.empty(GameFixtures.BLUE, 1).eliminate();
        assertThrows(IllegalStateException.class, () -> eliminated.add(GameFixtures.player(1)));
        assertEquals(eliminated, eliminated.eliminate());
        assertEquals(TeamSnapshot.empty(GameFixtures.BLUE, 1), TeamSnapshot.empty(GameFixtures.BLUE, 1));
        assertFalse(TeamSnapshot.empty(GameFixtures.BLUE, 1).equals("not a team"));
        final MatchSnapshot full = snapshot(MatchSnapshot.State.WAITING,
                Collections.<PlayerSession>emptyList(), Arrays.asList(
                new TeamSnapshot(GameFixtures.BLUE, 1, Collections.singletonList(GameFixtures.player(1)), true, false),
                new TeamSnapshot(GameFixtures.RED, 1, Collections.<PlayerId>emptyList(), true, true)),
                null, null, false);
        assertTrue(new TeamAssignmentPolicy().assign(GameFixtures.player(3), full,
                Optional.of(GameFixtures.BLUE)).isFailure());
    }

    @Test void playerSessionAndRulesCoverIdempotenceInvalidStatesAndBounds() {
        assertThrows(IllegalArgumentException.class, () -> new GameRules(2, 1, 1,
                Duration.ofSeconds(1), Duration.ofSeconds(1), Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class, () -> new GameRules(1, 257, 1,
                Duration.ofSeconds(1), Duration.ofSeconds(1), Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class, () -> new GameRules(1, 1, 601,
                Duration.ofSeconds(1), Duration.ofSeconds(1), Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class, () -> new GameRules(1, 1, 1,
                Duration.ofHours(2), Duration.ofSeconds(1), Duration.ofSeconds(1)));
        final PlayerSession waiting = PlayerSession.waiting(GameFixtures.BLUE,
                GameFixtures.state(GameFixtures.player(1)));
        assertFalse(waiting.disconnectedAt().isPresent());
        assertThrows(IllegalStateException.class, waiting::eliminate);
        final PlayerSession disconnected = waiting.disconnect(GameFixtures.NOW);
        assertTrue(disconnected == disconnected.disconnect(GameFixtures.NOW));
        assertEquals(PlayerSession.Status.WAITING,
                waiting.reconnect(GameFixtures.NOW, Duration.ofSeconds(1)).status());
        final PlayerSession restoring = waiting.beginRestoration();
        assertEquals(restoring, restoring.beginRestoration());
        assertThrows(IllegalStateException.class, waiting::restored);
        final PlayerSession restored = restoring.restored();
        assertEquals(restored, restored.restored());
        assertFalse(restored.isParticipating());
    }

    @Test void engineConstructorCapacityAndHealthRemainingBranchesAreCovered() {
        final io.zartra.bedwars.game.spi.MatchRepository repository = new io.zartra.bedwars.game.spi.MatchRepository() {
            @Override public java.util.concurrent.CompletionStage<Optional<MatchSnapshot>> load(final io.zartra.bedwars.api.identity.MatchId id) {
                return java.util.concurrent.CompletableFuture.completedFuture(Optional.<MatchSnapshot>empty());
            }
            @Override public java.util.concurrent.CompletionStage<Boolean> save(final long revision, final MatchSnapshot snapshot) {
                return java.util.concurrent.CompletableFuture.completedFuture(Boolean.TRUE);
            }
            @Override public java.util.concurrent.CompletionStage<Boolean> commitCompletion(final long revision,
                    final MatchSnapshot snapshot, final IdempotencyKey key) { return save(revision, snapshot); }
        };
        assertThrows(IllegalArgumentException.class, () -> new GameEngineService(0, repository, transition -> { }));
        assertThrows(IllegalArgumentException.class, () -> new GameEngineService(1001, repository, transition -> { }));
        final GameEngineService service = new GameEngineService(2, repository, transition -> { });
        assertTrue(service.health().hasCapacity());
        assertEquals(2, service.health().capacity());
        assertFalse(service.inspect(GameFixtures.match(55)).isPresent());
        service.register(GameFixtures.machine());
        service.register(new MatchStateMachine(GameFixtures.match(2), GameFixtures.arena(2),
                GameFixtures.rules(), teams(), GameFixtures.NOW));
        assertThrows(IllegalStateException.class, () -> service.register(new MatchStateMachine(
                GameFixtures.match(3), GameFixtures.arena(3), GameFixtures.rules(), teams(), GameFixtures.NOW)));
    }

    private static void apply(final MatchStateMachine machine, final MatchCommand command) {
        assertTrue(machine.apply(command, GameFixtures.NOW).isSuccess());
    }
    private static List<TeamSnapshot> teams() {
        return Arrays.asList(TeamSnapshot.empty(GameFixtures.BLUE, 2), TeamSnapshot.empty(GameFixtures.RED, 2));
    }
    private static List<TeamSnapshot> teamsWith(final PlayerId player) {
        return Arrays.asList(new TeamSnapshot(GameFixtures.BLUE, 2, Collections.singletonList(player), true, false),
                TeamSnapshot.empty(GameFixtures.RED, 2));
    }
    private static MatchSnapshot snapshot(final MatchSnapshot.State state,
            final List<PlayerSession> sessions, final List<TeamSnapshot> teams,
            final DefinitionId outcome, final IdempotencyKey key, final boolean committed) {
        return new MatchSnapshot(GameFixtures.match(1), GameFixtures.arena(1), 1L, state,
                state == MatchSnapshot.State.COUNTDOWN ? 1 : 0, teams, sessions,
                outcome, key, committed, GameFixtures.NOW);
    }
}
