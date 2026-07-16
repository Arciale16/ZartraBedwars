package io.zartra.bedwars.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.game.application.GameEngineService;
import io.zartra.bedwars.game.model.MatchCommand;
import io.zartra.bedwars.game.model.MatchSnapshot;
import io.zartra.bedwars.game.model.MatchStateMachine;
import io.zartra.bedwars.game.model.MatchTransition;
import io.zartra.bedwars.game.spi.MatchRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.Test;

class GameEngineServiceTest {
    @Test void persistsPublishesBoundsAndInspects() {
        final MemoryRepository repository = new MemoryRepository();
        final java.util.List<MatchTransition> events = new java.util.ArrayList<MatchTransition>();
        final GameEngineService service = new GameEngineService(1, repository, events::add);
        final MatchStateMachine machine = GameFixtures.machine();
        service.register(machine);
        assertThrows(IllegalStateException.class, () -> service.register(machine));
        assertEquals(1, service.health().registered());
        assertFalse(service.health().hasCapacity());
        final Result<MatchTransition> outcome = service.apply(machine.snapshot().matchId(),
                MatchCommand.admit(GameFixtures.BLUE, GameFixtures.state(GameFixtures.player(1))),
                GameFixtures.NOW).toCompletableFuture().join();
        assertTrue(outcome.isSuccess());
        assertEquals(1, events.size());
        assertEquals(1L, service.inspect(machine.snapshot().matchId()).get().revision());
        assertTrue(service.unregister(machine.snapshot().matchId()).isPresent());
        assertFalse(service.unregister(machine.snapshot().matchId()).isPresent());
    }

    @Test void rejectsUnknownAndOverlappingCommandsWithoutBlocking() {
        final MemoryRepository repository = new MemoryRepository();
        final CompletableFuture<Boolean> write = new CompletableFuture<Boolean>();
        repository.next = write;
        final GameEngineService service = new GameEngineService(2, repository, transition -> { });
        final MatchStateMachine machine = GameFixtures.machine();
        service.register(machine);
        final CompletionStage<Result<MatchTransition>> pending = service.apply(machine.snapshot().matchId(),
                MatchCommand.admit(GameFixtures.BLUE, GameFixtures.state(GameFixtures.player(1))), GameFixtures.NOW);
        assertEquals(1, service.health().busy());
        assertTrue(service.apply(machine.snapshot().matchId(), MatchCommand.startCountdown(), GameFixtures.NOW)
                .toCompletableFuture().join().isFailure());
        assertThrows(IllegalStateException.class, () -> service.unregister(machine.snapshot().matchId()));
        assertTrue(service.apply(GameFixtures.match(99), MatchCommand.startCountdown(), GameFixtures.NOW)
                .toCompletableFuture().join().isFailure());
        write.complete(true);
        assertTrue(pending.toCompletableFuture().join().isSuccess());
    }

    @Test void rollsBackOnRejectedExceptionalAndThrowingWrites() {
        final MemoryRepository repository = new MemoryRepository();
        final GameEngineService service = new GameEngineService(2, repository, transition -> { throw new IllegalStateException("isolated"); });
        final MatchStateMachine machine = GameFixtures.machine();
        service.register(machine);
        repository.next = CompletableFuture.completedFuture(false);
        assertTrue(service.apply(machine.snapshot().matchId(),
                MatchCommand.admit(GameFixtures.BLUE, GameFixtures.state(GameFixtures.player(1))), GameFixtures.NOW)
                .toCompletableFuture().join().isFailure());
        assertEquals(0L, service.inspect(machine.snapshot().matchId()).get().revision());
        repository.next = new CompletableFuture<Boolean>();
        repository.next.completeExceptionally(new IllegalStateException("database unavailable"));
        assertTrue(service.apply(machine.snapshot().matchId(),
                MatchCommand.admit(GameFixtures.BLUE, GameFixtures.state(GameFixtures.player(1))), GameFixtures.NOW)
                .toCompletableFuture().join().isFailure());
        repository.throwOnWrite = true;
        assertTrue(service.apply(machine.snapshot().matchId(),
                MatchCommand.admit(GameFixtures.BLUE, GameFixtures.state(GameFixtures.player(1))), GameFixtures.NOW)
                .toCompletableFuture().join().isFailure());
    }

    @Test void recoversPersistedSnapshotAndRejectsMissingOrDuplicateRecovery() {
        final MemoryRepository repository = new MemoryRepository();
        final MatchStateMachine original = GameFixtures.machine();
        repository.values.put(original.snapshot().matchId(), original.snapshot());
        final GameEngineService service = new GameEngineService(1, repository, transition -> { });
        assertTrue(service.recover(original.snapshot().matchId(),
                snapshot -> MatchStateMachine.recover(snapshot, GameFixtures.rules()))
                .toCompletableFuture().join());
        assertFalse(service.recover(original.snapshot().matchId(),
                snapshot -> MatchStateMachine.recover(snapshot, GameFixtures.rules()))
                .toCompletableFuture().join());
        assertFalse(new GameEngineService(1, repository, transition -> { }).recover(
                GameFixtures.match(99), snapshot -> MatchStateMachine.recover(snapshot, GameFixtures.rules()))
                .toCompletableFuture().join());
    }

    @Test void completionUsesAtomicPort() {
        final MemoryRepository repository = new MemoryRepository();
        final GameEngineService service = new GameEngineService(1, repository, transition -> { });
        final MatchStateMachine machine = GameFixtures.machine();
        service.register(machine);
        apply(service, machine, MatchCommand.admit(GameFixtures.BLUE, GameFixtures.state(GameFixtures.player(1))));
        apply(service, machine, MatchCommand.forceStart());
        final IdempotencyKey key = IdempotencyKey.of("zartra", "completion/service");
        apply(service, machine, MatchCommand.complete(io.zartra.bedwars.api.identity.DefinitionId.of("zartra", "outcome/draw"), key));
        apply(service, machine, MatchCommand.commitCompletion(key));
        assertEquals(1, repository.completions);
    }

    private static void apply(final GameEngineService service, final MatchStateMachine machine,
                              final MatchCommand command) {
        assertTrue(service.apply(machine.snapshot().matchId(), command, GameFixtures.NOW)
                .toCompletableFuture().join().isSuccess());
    }

    private static final class MemoryRepository implements MatchRepository {
        private final Map<MatchId, MatchSnapshot> values = new HashMap<MatchId, MatchSnapshot>();
        private CompletableFuture<Boolean> next;
        private boolean throwOnWrite;
        private int completions;
        @Override public CompletionStage<Optional<MatchSnapshot>> load(final MatchId matchId) {
            return CompletableFuture.completedFuture(Optional.ofNullable(values.get(matchId)));
        }
        @Override public CompletionStage<Boolean> save(final long previousRevision, final MatchSnapshot snapshot) {
            if (throwOnWrite) { throw new IllegalStateException("synchronous port failure"); }
            if (next != null) {
                final CompletableFuture<Boolean> answer = next;
                next = null;
                return answer;
            }
            values.put(snapshot.matchId(), snapshot);
            return CompletableFuture.completedFuture(true);
        }
        @Override public CompletionStage<Boolean> commitCompletion(final long previousRevision,
                                                                  final MatchSnapshot snapshot,
                                                                  final IdempotencyKey key) {
            completions++;
            return save(previousRevision, snapshot);
        }
    }
}
