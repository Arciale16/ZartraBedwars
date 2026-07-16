package io.zartra.bedwars.game.application;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.api.result.ApiError;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.game.model.MatchCommand;
import io.zartra.bedwars.game.model.MatchSnapshot;
import io.zartra.bedwars.game.model.MatchStateMachine;
import io.zartra.bedwars.game.model.MatchTransition;
import io.zartra.bedwars.game.spi.GameEventSink;
import io.zartra.bedwars.game.spi.MatchRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

/** Bounded shared-server registry and serialized persistence coordinator. */
public final class GameEngineService {
    private static final ApiError UNKNOWN = error("unknown_match", false);
    private static final ApiError BUSY = error("match_busy", true);
    private static final ApiError PERSISTENCE = error("persistence_failure", true);
    private static final ApiError REVISION = error("persistence_revision_conflict", true);
    private final int capacity;
    private final MatchRepository repository;
    private final GameEventSink events;
    private final Map<MatchId, RuntimeEntry> entries = new LinkedHashMap<MatchId, RuntimeEntry>();

    /** Creates a registry with an explicit hard match bound. */
    public GameEngineService(final int capacity, final MatchRepository repository,
                             final GameEventSink events) {
        if (capacity < 1 || capacity > 1000) {
            throw new IllegalArgumentException("capacity must be between 1 and 1000");
        }
        this.capacity = capacity;
        this.repository = Objects.requireNonNull(repository, "repository");
        this.events = Objects.requireNonNull(events, "events");
    }

    /** Registers a fully constructed aggregate; duplicate registration is rejected. */
    public synchronized void register(final MatchStateMachine machine) {
        Objects.requireNonNull(machine, "machine");
        final MatchId id = machine.snapshot().matchId();
        if (entries.containsKey(id)) { throw new IllegalStateException("match already registered"); }
        if (entries.size() >= capacity) { throw new IllegalStateException("match capacity reached"); }
        entries.put(id, new RuntimeEntry(machine));
    }

    /** Removes an idle aggregate and returns its final snapshot. */
    public synchronized Optional<MatchSnapshot> unregister(final MatchId matchId) {
        final RuntimeEntry entry = entries.get(Objects.requireNonNull(matchId, "matchId"));
        if (entry == null) { return Optional.empty(); }
        if (entry.busy) { throw new IllegalStateException("cannot unregister a busy match"); }
        entries.remove(matchId);
        return Optional.of(entry.machine.snapshot());
    }

    /** @return current snapshot without platform or storage access */
    public synchronized Optional<MatchSnapshot> inspect(final MatchId matchId) {
        final RuntimeEntry entry = entries.get(Objects.requireNonNull(matchId, "matchId"));
        return entry == null ? Optional.<MatchSnapshot>empty()
                : Optional.of(entry.machine.snapshot());
    }

    /**
     * Applies and asynchronously persists one command. A failed write rolls the in-memory
     * aggregate back to the exact prior snapshot before accepting another command.
     */
    public CompletionStage<Result<MatchTransition>> apply(
            final MatchId matchId, final MatchCommand command, final Instant now) {
        Objects.requireNonNull(matchId, "matchId");
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(now, "now");
        final RuntimeEntry entry;
        final MatchSnapshot before;
        final Result<MatchTransition> applied;
        synchronized (this) {
            entry = entries.get(matchId);
            if (entry == null) { return completed(Result.<MatchTransition>failure(UNKNOWN)); }
            if (entry.busy) { return completed(Result.<MatchTransition>failure(BUSY)); }
            before = entry.machine.snapshot();
            applied = entry.machine.apply(command, before.revision(), now);
            if (applied.isFailure() || applied.requireValue().duplicate()) {
                return completed(applied);
            }
            entry.busy = true;
        }
        final MatchTransition transition = applied.requireValue();
        final Supplier<CompletionStage<Boolean>> write = command.type()
                == MatchCommand.Type.COMMIT_COMPLETION
                ? new Supplier<CompletionStage<Boolean>>() {
                    @Override public CompletionStage<Boolean> get() {
                        return repository.commitCompletion(before.revision(), transition.after(),
                                command.completionKey().get());
                    }
                } : new Supplier<CompletionStage<Boolean>>() {
                    @Override public CompletionStage<Boolean> get() {
                        return repository.save(before.revision(), transition.after());
                    }
                };
        final CompletableFuture<Result<MatchTransition>> outcome =
                new CompletableFuture<Result<MatchTransition>>();
        final CompletionStage<Boolean> stage;
        try { stage = Objects.requireNonNull(write.get(), "repository stage"); }
        catch (RuntimeException failure) {
            finishFailure(entry, before, outcome, PERSISTENCE);
            return outcome;
        }
        stage.whenComplete((accepted, failure) -> {
            if (failure != null) {
                finishFailure(entry, before, outcome, PERSISTENCE);
            } else if (!Boolean.TRUE.equals(accepted)) {
                finishFailure(entry, before, outcome, REVISION);
            } else {
                synchronized (GameEngineService.this) { entry.busy = false; }
                try { events.publish(transition); }
                catch (RuntimeException isolated) { /* persisted authority is unaffected */ }
                outcome.complete(Result.success(transition));
            }
        });
        return outcome;
    }

    /** Recovers one snapshot when no in-memory aggregate with the same ID is registered. */
    public CompletionStage<Boolean> recover(
            final MatchId matchId,
            final java.util.function.Function<MatchSnapshot, MatchStateMachine> factory) {
        Objects.requireNonNull(matchId, "matchId");
        Objects.requireNonNull(factory, "factory");
        final CompletableFuture<Boolean> answer = new CompletableFuture<Boolean>();
        repository.load(matchId).whenComplete((loaded, failure) -> {
            if (failure != null || loaded == null || !loaded.isPresent()) {
                answer.complete(false);
                return;
            }
            try {
                synchronized (GameEngineService.this) {
                    if (entries.containsKey(matchId) || entries.size() >= capacity) {
                        answer.complete(false);
                        return;
                    }
                    entries.put(matchId, new RuntimeEntry(factory.apply(loaded.get())));
                }
                answer.complete(true);
            } catch (RuntimeException invalid) { answer.completeExceptionally(invalid); }
        });
        return answer;
    }

    /** @return immutable operational counters */
    public synchronized Health health() {
        int busy = 0;
        for (RuntimeEntry entry : entries.values()) { if (entry.busy) { busy++; } }
        return new Health(entries.size(), busy, capacity);
    }

    private synchronized void finishFailure(
            final RuntimeEntry entry, final MatchSnapshot before,
            final CompletableFuture<Result<MatchTransition>> outcome, final ApiError error) {
        entry.machine = MatchStateMachine.recover(before, entry.machine.rules(),
                entry.machine.victoryEvaluator());
        entry.busy = false;
        outcome.complete(Result.<MatchTransition>failure(error));
    }

    private static <T> CompletionStage<T> completed(final T value) {
        return CompletableFuture.completedFuture(value);
    }
    private static ApiError error(final String path, final boolean retryable) {
        return ApiError.of(DefinitionId.of("zartra", "game/" + path), "game." + path,
                retryable ? ApiError.RetryDisposition.RETRYABLE
                        : ApiError.RetryDisposition.PERMANENT);
    }

    private static final class RuntimeEntry {
        private MatchStateMachine machine;
        private boolean busy;
        private RuntimeEntry(final MatchStateMachine machine) { this.machine = machine; }
    }

    /** Immutable shared-server health view. */
    public static final class Health {
        private final int registered;
        private final int busy;
        private final int capacity;
        private Health(final int registered, final int busy, final int capacity) {
            this.registered = registered;
            this.busy = busy;
            this.capacity = capacity;
        }
        /** @return registered match count */ public int registered() { return registered; }
        /** @return matches waiting for persistence */ public int busy() { return busy; }
        /** @return configured hard bound */ public int capacity() { return capacity; }
        /** @return whether another match can be registered */ public boolean hasCapacity() { return registered < capacity; }
    }
}
