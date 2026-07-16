package io.zartra.bedwars.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.game.model.GamePhasePolicy;
import io.zartra.bedwars.game.model.MatchCommand;
import io.zartra.bedwars.game.model.MatchSnapshot;
import io.zartra.bedwars.game.model.MatchStateMachine;
import io.zartra.bedwars.game.model.MatchTransition;
import io.zartra.bedwars.game.model.PlayerSession;
import io.zartra.bedwars.game.model.TeamAssignmentPolicy;
import io.zartra.bedwars.game.model.TeamSnapshot;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MatchStateMachineTest {
    @Test void fullLifecycleIsDeterministicAndIdempotent() {
        final MatchStateMachine machine = GameFixtures.machine();
        final PlayerId one = GameFixtures.player(1);
        final PlayerId two = GameFixtures.player(2);
        success(machine.apply(MatchCommand.admit(GameFixtures.BLUE, GameFixtures.state(one)), GameFixtures.NOW));
        assertTrue(success(machine.apply(MatchCommand.admit(GameFixtures.BLUE, GameFixtures.state(one)), GameFixtures.NOW)).duplicate());
        success(machine.apply(MatchCommand.admit(GameFixtures.RED, GameFixtures.state(two)), GameFixtures.NOW));
        success(machine.apply(MatchCommand.startCountdown(), GameFixtures.NOW));
        assertEquals(2, machine.snapshot().countdownRemaining());
        success(machine.apply(MatchCommand.tick(), GameFixtures.NOW.plusSeconds(1)));
        success(machine.apply(MatchCommand.tick(), GameFixtures.NOW.plusSeconds(2)));
        assertEquals(MatchSnapshot.State.PLAYING, machine.snapshot().state());
        success(machine.apply(MatchCommand.disconnect(one), GameFixtures.NOW.plusSeconds(3)));
        success(machine.apply(MatchCommand.reconnect(one), GameFixtures.NOW.plusSeconds(20)));
        success(machine.apply(MatchCommand.destroyBed(GameFixtures.RED), GameFixtures.NOW.plusSeconds(21)));
        assertTrue(success(machine.apply(MatchCommand.destroyBed(GameFixtures.RED), GameFixtures.NOW.plusSeconds(21))).duplicate());
        final MatchTransition elimination = success(machine.apply(
                MatchCommand.eliminate(two), GameFixtures.NOW.plusSeconds(22)));
        assertTrue(machine.snapshot().team(GameFixtures.RED).get().eliminated());
        final IdempotencyKey key = IdempotencyKey.of("zartra", "match/1");
        assertTrue(elimination.completionIntent().isPresent());
        assertEquals(GameFixtures.BLUE,
                elimination.completionIntent().get().winningTeamId());
        final DefinitionId victory = elimination.completionIntent().get().outcome();
        success(machine.apply(MatchCommand.complete(victory, key), GameFixtures.NOW.plusSeconds(23)));
        assertTrue(success(machine.apply(MatchCommand.complete(victory, key), GameFixtures.NOW.plusSeconds(23))).duplicate());
        success(machine.apply(MatchCommand.commitCompletion(key), GameFixtures.NOW.plusSeconds(24)));
        assertTrue(success(machine.apply(MatchCommand.commitCompletion(key), GameFixtures.NOW.plusSeconds(24))).duplicate());
        assertEquals(PlayerSession.Status.RESTORING, machine.snapshot().session(one).get().status());
        success(machine.apply(MatchCommand.restore(one), GameFixtures.NOW.plusSeconds(25)));
        assertTrue(success(machine.apply(MatchCommand.restore(one), GameFixtures.NOW.plusSeconds(25))).duplicate());
        success(machine.apply(MatchCommand.restore(two), GameFixtures.NOW.plusSeconds(26)));
        success(machine.apply(MatchCommand.finishReset(), GameFixtures.NOW.plusSeconds(27)));
        assertEquals(MatchSnapshot.State.WAITING, machine.snapshot().state());
        assertTrue(machine.snapshot().sessions().isEmpty());
        assertTrue(machine.snapshot().teams().get(0).bedPresent());
    }

    @Test void invalidTransitionsRevisionsAndReconnectExpiryFailClosed() {
        final MatchStateMachine machine = GameFixtures.machine();
        assertTrue(machine.apply(MatchCommand.startCountdown(), GameFixtures.NOW).isFailure());
        final PlayerId one = GameFixtures.player(1);
        success(machine.apply(MatchCommand.admit(GameFixtures.BLUE, GameFixtures.state(one)), GameFixtures.NOW));
        assertTrue(machine.apply(MatchCommand.remove(GameFixtures.player(9)), GameFixtures.NOW).isFailure());
        assertTrue(machine.apply(MatchCommand.forceStart(), 0L, GameFixtures.NOW).isFailure());
        success(machine.apply(MatchCommand.forceStart(), GameFixtures.NOW));
        success(machine.apply(MatchCommand.disconnect(one), GameFixtures.NOW));
        assertTrue(machine.apply(MatchCommand.reconnect(one), GameFixtures.NOW.plusSeconds(31)).isFailure());
        assertTrue(machine.apply(MatchCommand.destroyBed(DefinitionId.of("zartra", "team/missing")), GameFixtures.NOW).isFailure());
        assertTrue(machine.apply(MatchCommand.finishReset(), GameFixtures.NOW).isFailure());
    }

    @Test void countdownCancelsAfterRemovalAndExplicitCancellationWorks() {
        final MatchStateMachine machine = GameFixtures.machine();
        success(machine.apply(MatchCommand.admit(GameFixtures.BLUE, GameFixtures.state(GameFixtures.player(1))), GameFixtures.NOW));
        success(machine.apply(MatchCommand.admit(GameFixtures.RED, GameFixtures.state(GameFixtures.player(2))), GameFixtures.NOW));
        success(machine.apply(MatchCommand.startCountdown(), GameFixtures.NOW));
        success(machine.apply(MatchCommand.cancelCountdown(), GameFixtures.NOW));
        success(machine.apply(MatchCommand.startCountdown(), GameFixtures.NOW));
        success(machine.apply(MatchCommand.remove(GameFixtures.player(2)), GameFixtures.NOW));
        assertEquals(MatchSnapshot.State.WAITING, machine.snapshot().state());
    }

    @Test void assignmentHonoursPreferenceBalanceAndExistingMembership() {
        final MatchStateMachine machine = GameFixtures.machine();
        final TeamAssignmentPolicy policy = new TeamAssignmentPolicy();
        final PlayerId one = GameFixtures.player(1);
        assertEquals(GameFixtures.RED, policy.assign(one, machine.snapshot(), Optional.of(GameFixtures.RED)).requireValue());
        success(machine.apply(MatchCommand.admit(GameFixtures.RED, GameFixtures.state(one)), GameFixtures.NOW));
        assertEquals(GameFixtures.RED, policy.assign(one, machine.snapshot(), Optional.<DefinitionId>empty()).requireValue());
        assertEquals(GameFixtures.BLUE, policy.assign(GameFixtures.player(2), machine.snapshot(), Optional.<DefinitionId>empty()).requireValue());
    }

    @Test void phaseScheduleFiresExactlyOnceAndResets() {
        final GamePhasePolicy.ScheduledEvent border = new GamePhasePolicy.ScheduledEvent(
                DefinitionId.of("zartra", "phase/border"), GamePhasePolicy.Type.BORDER,
                Duration.ofSeconds(10), null);
        final GamePhasePolicy.ScheduledEvent custom = new GamePhasePolicy.ScheduledEvent(
                DefinitionId.of("zartra", "phase/custom"), GamePhasePolicy.Type.CUSTOM,
                Duration.ofSeconds(20), DefinitionId.of("zartra", "event/meteor"));
        final GamePhasePolicy policy = new GamePhasePolicy(Arrays.asList(custom, border));
        assertTrue(policy.due(GameFixtures.NOW, GameFixtures.NOW.plusSeconds(9)).isEmpty());
        assertEquals(border, policy.due(GameFixtures.NOW, GameFixtures.NOW.plusSeconds(10)).get(0));
        assertTrue(policy.due(GameFixtures.NOW, GameFixtures.NOW.plusSeconds(10)).isEmpty());
        assertEquals(custom, policy.due(GameFixtures.NOW, GameFixtures.NOW.plusSeconds(30)).get(0));
        assertTrue(custom.payload().isPresent());
        policy.reset();
        assertEquals(2, policy.due(GameFixtures.NOW, GameFixtures.NOW.plusSeconds(30)).size());
        assertThrows(IllegalArgumentException.class, () -> policy.due(GameFixtures.NOW, GameFixtures.NOW.minusSeconds(1)));
    }

    @Test void constructorsRejectMalformedAggregatesAndEvents() {
        assertThrows(IllegalArgumentException.class, () -> new TeamSnapshot(GameFixtures.RED, 1,
                Arrays.asList(GameFixtures.player(1), GameFixtures.player(2)), true, false));
        assertThrows(IllegalArgumentException.class, () -> new GamePhasePolicy.ScheduledEvent(
                DefinitionId.of("zartra", "phase/invalid"), GamePhasePolicy.Type.CUSTOM,
                Duration.ZERO, null));
        assertThrows(IllegalArgumentException.class, () -> new GamePhasePolicy(Arrays.asList(
                new GamePhasePolicy.ScheduledEvent(DefinitionId.of("zartra", "phase/a"), GamePhasePolicy.Type.TIMEOUT, Duration.ZERO, null),
                new GamePhasePolicy.ScheduledEvent(DefinitionId.of("zartra", "phase/b"), GamePhasePolicy.Type.BORDER, Duration.ZERO, null))));
        assertFalse(GameFixtures.machine().snapshot().session(GameFixtures.player(99)).isPresent());
    }

    private static MatchTransition success(final Result<MatchTransition> result) {
        assertTrue(result.isSuccess(), () -> String.valueOf(result.error()));
        return result.requireValue();
    }
}
