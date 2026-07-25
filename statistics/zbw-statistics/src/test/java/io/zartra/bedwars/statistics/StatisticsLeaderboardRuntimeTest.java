package io.zartra.bedwars.statistics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.statistics.leaderboard.StatisticsLeaderboardRuntime;
import io.zartra.bedwars.statistics.model.StatisticId;
import io.zartra.bedwars.statistics.model.StatisticScope;
import io.zartra.bedwars.storage.api.UnitOfWork;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** M15 ranking order, filters, paging, rebuild and deterministic consistency evidence. */
final class StatisticsLeaderboardRuntimeTest {
    private static final StatisticId WINS = StatisticId.of("zartra", "wins");
    private static final StatisticScope GLOBAL = StatisticScope.of("zartra", "global");
    private static final PlayerId FIRST = PlayerId.of(new UUID(0, 201));
    private static final PlayerId SECOND = PlayerId.of(new UUID(0, 202));
    private static final PlayerId THIRD = PlayerId.of(new UUID(0, 203));

    @Test
    void playerAndSeasonalRankingsUseStableTieBreakingAndBoundedPagination() {
        final MemoryStore store = new MemoryStore(Arrays.asList(
                aggregate(StatisticsLeaderboardRuntime.Subject.player(SECOND), 9),
                aggregate(StatisticsLeaderboardRuntime.Subject.player(FIRST), 9),
                aggregate(StatisticsLeaderboardRuntime.Subject.player(THIRD), 3)));
        final StatisticsLeaderboardRuntime runtime = new StatisticsLeaderboardRuntime(store);
        final StatisticsLeaderboardRuntime.Page first = runtime.query(new MemoryUnit(), query(
                StatisticsLeaderboardRuntime.Kind.PLAYER, 0, 2,
                Collections.<StatisticsLeaderboardRuntime.Subject>emptyList())).requireValue();
        assertEquals(FIRST, first.entries().get(0).subject().playerId().get());
        assertEquals(1, first.entries().get(0).rank());
        assertEquals(SECOND, first.entries().get(1).subject().playerId().get());
        assertTrue(first.hasNext());
        final StatisticsLeaderboardRuntime.Page second = runtime.query(new MemoryUnit(), query(
                StatisticsLeaderboardRuntime.Kind.PLAYER, 2, 2,
                Collections.<StatisticsLeaderboardRuntime.Subject>emptyList())).requireValue();
        assertEquals(3, second.entries().get(0).rank());
        assertFalse(second.hasNext());
    }

    @Test
    void teamFiltersAndEmptyLeaderboardsRemainDeterministic() {
        final StatisticsLeaderboardRuntime.Subject red = StatisticsLeaderboardRuntime.Subject.team(
                DefinitionId.of("zartra", "team/red"));
        final StatisticsLeaderboardRuntime.Subject blue = StatisticsLeaderboardRuntime.Subject.team(
                DefinitionId.of("zartra", "team/blue"));
        final StatisticsLeaderboardRuntime runtime = new StatisticsLeaderboardRuntime(new MemoryStore(Arrays.asList(
                aggregate(red, 2), aggregate(blue, 5))));
        final StatisticsLeaderboardRuntime.Page filtered = runtime.query(new MemoryUnit(), query(
                StatisticsLeaderboardRuntime.Kind.TEAM, 0, 10, Collections.singletonList(red)))
                .requireValue();
        assertEquals(1, filtered.entries().size());
        assertEquals(red, filtered.entries().get(0).subject());
        final StatisticsLeaderboardRuntime runtimeEmpty = new StatisticsLeaderboardRuntime(
                new MemoryStore(Collections.<StatisticsLeaderboardRuntime.Aggregate>emptyList()));
        assertTrue(runtimeEmpty.query(new MemoryUnit(), query(StatisticsLeaderboardRuntime.Kind.PLAYER,
                0, 10, Collections.<StatisticsLeaderboardRuntime.Subject>emptyList()))
                .requireValue().entries().isEmpty());
    }

    @Test
    void rebuildAndConsistencyUseExistingPersistedAggregateRows() {
        final MemoryStore store = new MemoryStore(Collections.singletonList(
                aggregate(StatisticsLeaderboardRuntime.Subject.seasonalPlayer(FIRST), 7)));
        final StatisticsLeaderboardRuntime runtime = new StatisticsLeaderboardRuntime(store);
        final StatisticsLeaderboardRuntime.Query query = query(
                StatisticsLeaderboardRuntime.Kind.SEASONAL, 0, 10,
                Collections.<StatisticsLeaderboardRuntime.Subject>emptyList());
        final StatisticsLeaderboardRuntime.RebuildResult rebuilt = runtime.rebuild(new MemoryUnit(),
                new StatisticsLeaderboardRuntime.RebuildRequest(query, 100)).requireValue();
        assertEquals(1, rebuilt.rebuiltRows());
        assertTrue(runtime.check(new MemoryUnit(), query).requireValue().consistent());
        assertThrows(IllegalArgumentException.class, () -> new StatisticsLeaderboardRuntime.RebuildRequest(
                query, 0));
    }

    private static StatisticsLeaderboardRuntime.Query query(
            final StatisticsLeaderboardRuntime.Kind kind, final int offset, final int pageSize,
            final List<StatisticsLeaderboardRuntime.Subject> filter) {
        return new StatisticsLeaderboardRuntime.Query(kind, WINS, GLOBAL,
                StatisticsLeaderboardRuntime.Sort.DESCENDING, offset, pageSize, filter);
    }

    private static StatisticsLeaderboardRuntime.Aggregate aggregate(
            final StatisticsLeaderboardRuntime.Subject subject, final long value) {
        return new StatisticsLeaderboardRuntime.Aggregate(subject, WINS, GLOBAL, value);
    }

    private static final class MemoryStore implements StatisticsLeaderboardRuntime.Store {
        private final List<StatisticsLeaderboardRuntime.Aggregate> values;
        private MemoryStore(final List<StatisticsLeaderboardRuntime.Aggregate> values) {
            this.values = values;
        }
        @Override
        public Result<List<StatisticsLeaderboardRuntime.Aggregate>> load(final UnitOfWork unit,
                final StatisticsLeaderboardRuntime.Query query) {
            return Result.success(values);
        }
        @Override
        public Result<StatisticsLeaderboardRuntime.RebuildResult> rebuild(final UnitOfWork unit,
                final StatisticsLeaderboardRuntime.RebuildRequest request) {
            return Result.success(new StatisticsLeaderboardRuntime.RebuildResult(values.size(), false));
        }
    }

    private static final class MemoryUnit implements UnitOfWork {
        @Override public State state() { return State.ACTIVE; }
        @Override public Result<State> commit() { return Result.success(State.COMMITTED); }
        @Override public Result<State> rollback() { return Result.success(State.ROLLED_BACK); }
        @Override public void close() { }
    }
}
