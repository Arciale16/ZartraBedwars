package io.zartra.bedwars.statistics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.EventId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.result.ApiError;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.statistics.model.MatchStatistic;
import io.zartra.bedwars.statistics.model.PlayerStatistic;
import io.zartra.bedwars.statistics.model.SeasonalStatistic;
import io.zartra.bedwars.statistics.model.StatisticAudit;
import io.zartra.bedwars.statistics.model.StatisticCategory;
import io.zartra.bedwars.statistics.model.StatisticDefinition;
import io.zartra.bedwars.statistics.model.StatisticId;
import io.zartra.bedwars.statistics.model.StatisticScope;
import io.zartra.bedwars.statistics.model.TeamStatistic;
import io.zartra.bedwars.statistics.projection.StatisticProjection;
import io.zartra.bedwars.statistics.runtime.MatchStatisticsProjection;
import io.zartra.bedwars.statistics.runtime.SeasonWindow;
import io.zartra.bedwars.statistics.runtime.SeasonalStatisticsProjection;
import io.zartra.bedwars.statistics.runtime.StatisticsProjectionEngine;
import io.zartra.bedwars.statistics.runtime.TeamStatisticsProjection;
import io.zartra.bedwars.storage.api.RecordRevision;
import io.zartra.bedwars.storage.api.UnitOfWork;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Behavioral coverage for M15 aggregation, idempotency and optimistic-conflict paths. */
final class StatisticsProjectionRuntimeTest {
    private static final Instant NOW = Instant.parse("2026-07-25T10:00:00Z");
    private static final PlayerId PLAYER = PlayerId.of(new UUID(0, 301));
    private static final MatchId MATCH = MatchId.of(new UUID(0, 302));
    private static final DefinitionId TEAM = DefinitionId.of("zartra", "team/red");
    private static final StatisticId WINS = StatisticId.of("zartra", "wins");
    private static final StatisticScope GLOBAL = StatisticScope.of("zartra", "global");
    private static final StatisticScope SEASON = StatisticScope.of("zartra", "season/summer");
    private static final StatisticAudit AUDIT = new StatisticAudit("statistics-test",
            CorrelationId.of(new UUID(0, 303)), NOW);
    private static final ApiError FAILURE = ApiError.of(
            DefinitionId.of("zartra", "statistics/failure"), "statistics.failure",
            ApiError.RetryDisposition.RETRYABLE);

    @Test
    void playerProjectionSupportsAllAggregationModesAndExistingRevisions() {
        final PlayerStore store = new PlayerStore();
        final StatisticsProjectionEngine engine = new StatisticsProjectionEngine(store);

        assertEquals(4L, project(engine, definition(StatisticDefinition.Aggregation.SUM),
                event(4, "sum-first")).statistic().get().value());
        store.current = playerStatistic(4, 3);
        assertEquals(7L, project(engine, definition(StatisticDefinition.Aggregation.SUM),
                event(3, "sum-existing")).statistic().get().value());
        assertEquals(3L, store.expected.value());

        store.current = playerStatistic(7, 5);
        assertEquals(7L, project(engine, definition(StatisticDefinition.Aggregation.MAXIMUM),
                event(2, "maximum-low")).statistic().get().value());
        assertEquals(11L, project(engine, definition(StatisticDefinition.Aggregation.MAXIMUM),
                event(11, "maximum-high")).statistic().get().value());

        store.current = playerStatistic(11, 7);
        assertEquals(2L, project(engine, definition(StatisticDefinition.Aggregation.LATEST),
                event(2, "latest")).statistic().get().value());
        assertEquals(NOW, store.saved.audit().recordedAt());
    }

    @Test
    void playerProjectionReportsDuplicatesAndTypedStoreFailures() {
        final PlayerStore store = new PlayerStore();
        final StatisticsProjectionEngine engine = new StatisticsProjectionEngine(store);
        store.claimed = false;
        final StatisticsProjectionEngine.Outcome duplicate = engine.project(new MemoryUnit(), PLAYER,
                definition(StatisticDefinition.Aggregation.SUM), event(1, "duplicate"), NOW)
                .requireValue();
        assertTrue(duplicate.duplicate());
        assertFalse(duplicate.statistic().isPresent());

        store.claimFailure = true;
        assertTrue(engine.project(new MemoryUnit(), PLAYER,
                definition(StatisticDefinition.Aggregation.SUM), event(1, "claim-failure"), NOW)
                .isFailure());
        store.claimFailure = false;
        store.claimed = true;
        store.findFailure = true;
        assertTrue(engine.project(new MemoryUnit(), PLAYER,
                definition(StatisticDefinition.Aggregation.SUM), event(1, "find-failure"), NOW)
                .isFailure());
        store.findFailure = false;
        store.saveFailure = true;
        assertTrue(engine.project(new MemoryUnit(), PLAYER,
                definition(StatisticDefinition.Aggregation.SUM), event(1, "save-failure"), NOW)
                .isFailure());
    }

    @Test
    void playerProjectionRejectsMismatchedDefinitionsAndOverflow() {
        final StatisticsProjectionEngine engine = new StatisticsProjectionEngine(new PlayerStore());
        final StatisticDefinition other = new StatisticDefinition(
                StatisticId.of("zartra", "losses"), StatisticCategory.MATCH, 1,
                StatisticDefinition.Aggregation.SUM, AUDIT);
        assertThrows(IllegalArgumentException.class,
                () -> engine.project(new MemoryUnit(), PLAYER, other, event(1, "mismatch"), NOW));

        final PlayerStore store = new PlayerStore();
        store.current = playerStatistic(Long.MAX_VALUE, 1);
        assertThrows(ArithmeticException.class,
                () -> new StatisticsProjectionEngine(store).project(new MemoryUnit(), PLAYER,
                        definition(StatisticDefinition.Aggregation.SUM),
                        event(1, "overflow"), NOW));
    }

    @Test
    void matchProjectionExposesAppliedDuplicateConflictAndFailureOutcomes() {
        final MatchStore store = new MatchStore();
        final MatchStatisticsProjection projection = new MatchStatisticsProjection(store);
        final MatchStatistic statistic = matchStatistic(5);
        final MatchStatisticsProjection.Outcome applied = projection.project(new MemoryUnit(),
                statistic, RecordRevision.initial(), key("match-applied"), NOW).requireValue();
        assertEquals(MatchStatisticsProjection.Outcome.Status.APPLIED, applied.status());
        assertEquals(statistic, applied.value().get());

        store.claimed = false;
        final MatchStatisticsProjection.Outcome duplicate = projection.project(new MemoryUnit(),
                statistic, RecordRevision.initial(), key("match-duplicate"), NOW).requireValue();
        assertEquals(MatchStatisticsProjection.Outcome.Status.DUPLICATE, duplicate.status());
        assertFalse(duplicate.value().isPresent());

        store.claimed = true;
        store.saveFailure = true;
        assertEquals(MatchStatisticsProjection.Outcome.Status.REVISION_CONFLICT,
                projection.project(new MemoryUnit(), statistic, RecordRevision.initial(),
                        key("match-conflict"), NOW).requireValue().status());
        store.claimFailure = true;
        assertTrue(projection.project(new MemoryUnit(), statistic, RecordRevision.initial(),
                key("match-failure"), NOW).isFailure());
    }

    @Test
    void teamProjectionExposesAppliedDuplicateConflictAndFailureOutcomes() {
        final TeamStore store = new TeamStore();
        final TeamStatisticsProjection projection = new TeamStatisticsProjection(store);
        final TeamStatistic statistic = teamStatistic(8);
        final TeamStatisticsProjection.Outcome applied = projection.project(new MemoryUnit(),
                statistic, RecordRevision.initial(), key("team-applied"), NOW).requireValue();
        assertEquals(TeamStatisticsProjection.Outcome.Status.APPLIED, applied.status());
        assertEquals(statistic, applied.value().get());

        store.claimed = false;
        final TeamStatisticsProjection.Outcome duplicate = projection.project(new MemoryUnit(),
                statistic, RecordRevision.initial(), key("team-duplicate"), NOW).requireValue();
        assertEquals(TeamStatisticsProjection.Outcome.Status.DUPLICATE, duplicate.status());
        assertFalse(duplicate.value().isPresent());

        store.claimed = true;
        store.saveFailure = true;
        assertEquals(TeamStatisticsProjection.Outcome.Status.REVISION_CONFLICT,
                projection.project(new MemoryUnit(), statistic, RecordRevision.initial(),
                        key("team-conflict"), NOW).requireValue().status());
        store.claimFailure = true;
        assertTrue(projection.project(new MemoryUnit(), statistic, RecordRevision.initial(),
                key("team-failure"), NOW).isFailure());
    }

    @Test
    void seasonalProjectionEnforcesBoundariesAndExposesAllOutcomes() {
        final SeasonStore store = new SeasonStore();
        final SeasonalStatisticsProjection projection = new SeasonalStatisticsProjection(store);
        final SeasonalStatistic statistic = seasonalStatistic(12);
        final SeasonWindow window = new SeasonWindow(SEASON, 2, NOW.minusSeconds(60),
                NOW.plusSeconds(60));
        final SeasonalStatisticsProjection.Outcome applied = projection.project(new MemoryUnit(),
                statistic, RecordRevision.initial(), key("season-applied"), NOW, window)
                .requireValue();
        assertEquals(SeasonalStatisticsProjection.Outcome.Status.APPLIED, applied.status());
        assertEquals(statistic, applied.value().get());

        store.claimed = false;
        assertEquals(SeasonalStatisticsProjection.Outcome.Status.DUPLICATE,
                projection.project(new MemoryUnit(), statistic, RecordRevision.initial(),
                        key("season-duplicate"), NOW, window).requireValue().status());
        store.claimed = true;
        store.saveFailure = true;
        assertEquals(SeasonalStatisticsProjection.Outcome.Status.REVISION_CONFLICT,
                projection.project(new MemoryUnit(), statistic, RecordRevision.initial(),
                        key("season-conflict"), NOW, window).requireValue().status());
        store.claimFailure = true;
        assertTrue(projection.project(new MemoryUnit(), statistic, RecordRevision.initial(),
                key("season-failure"), NOW, window).isFailure());

        assertThrows(IllegalArgumentException.class,
                () -> projection.project(new MemoryUnit(), statistic, RecordRevision.initial(),
                        key("season-too-late"), NOW.plusSeconds(60), window));
        assertThrows(IllegalArgumentException.class,
                () -> projection.project(new MemoryUnit(),
                        new SeasonalStatistic(PLAYER, WINS,
                                StatisticScope.of("zartra", "season/other"), 1, AUDIT),
                        RecordRevision.initial(), key("season-mismatch"), NOW, window));
    }

    @Test
    void seasonWindowIsHalfOpenVersionedAndValidated() {
        final Instant start = NOW.minusSeconds(10);
        final Instant end = NOW.plusSeconds(10);
        final SeasonWindow window = new SeasonWindow(SEASON, 3, start, end);
        assertTrue(window.contains(start));
        assertTrue(window.contains(NOW));
        assertFalse(window.contains(end));
        assertFalse(window.contains(start.minusNanos(1)));
        assertEquals(SEASON, window.seasonId());
        assertEquals(3, window.definitionVersion());
        assertEquals(start, window.startsAt());
        assertEquals(end, window.endsAt());
        assertThrows(IllegalArgumentException.class,
                () -> new SeasonWindow(SEASON, 0, start, end));
        assertThrows(IllegalArgumentException.class,
                () -> new SeasonWindow(SEASON, 1, end, end));
        assertThrows(IllegalArgumentException.class,
                () -> new SeasonWindow(SEASON, 1, end, start));
        assertThrows(NullPointerException.class, () -> window.contains(null));
    }

    private static StatisticsProjectionEngine.Outcome project(
            final StatisticsProjectionEngine engine, final StatisticDefinition definition,
            final StatisticProjection.Event event) {
        final StatisticsProjectionEngine.Outcome outcome = engine.project(new MemoryUnit(), PLAYER,
                definition, event, NOW).requireValue();
        assertFalse(outcome.duplicate());
        return outcome;
    }

    private static StatisticDefinition definition(
            final StatisticDefinition.Aggregation aggregation) {
        return new StatisticDefinition(WINS, StatisticCategory.MATCH, 1, aggregation, AUDIT);
    }

    private static StatisticProjection.Event event(final long delta, final String key) {
        return new StatisticProjection.Event(EventId.of(new UUID(0, key.hashCode() & 0xffff)),
                key(key), StatisticProjection.Source.MATCH, WINS, GLOBAL, delta, AUDIT);
    }

    private static PlayerStatistic playerStatistic(final long value, final long revision) {
        return new PlayerStatistic(PLAYER, WINS, GLOBAL, value, RecordRevision.of(revision), AUDIT);
    }

    private static MatchStatistic matchStatistic(final long value) {
        return new MatchStatistic(MATCH, WINS, value, RecordRevision.of(1), AUDIT);
    }

    private static TeamStatistic teamStatistic(final long value) {
        return new TeamStatistic(MATCH, TEAM, WINS, value, RecordRevision.of(1), AUDIT);
    }

    private static SeasonalStatistic seasonalStatistic(final long value) {
        return new SeasonalStatistic(PLAYER, WINS, SEASON, value, RecordRevision.of(1), AUDIT);
    }

    private static IdempotencyKey key(final String value) {
        return IdempotencyKey.of("test", value);
    }

    private static final class PlayerStore implements StatisticsProjectionEngine.Store {
        private boolean claimed = true;
        private boolean claimFailure;
        private boolean findFailure;
        private boolean saveFailure;
        private PlayerStatistic current;
        private PlayerStatistic saved;
        private RecordRevision expected;

        @Override
        public Result<Boolean> claim(final UnitOfWork unitOfWork, final IdempotencyKey key,
                                     final Instant now) {
            return claimFailure ? Result.<Boolean>failure(FAILURE) : Result.success(claimed);
        }

        @Override
        public Result<Optional<PlayerStatistic>> find(final UnitOfWork unitOfWork,
                final PlayerId playerId, final StatisticId statisticId,
                final StatisticScope scope) {
            return findFailure ? Result.<Optional<PlayerStatistic>>failure(FAILURE)
                    : Result.success(Optional.ofNullable(current));
        }

        @Override
        public Result<PlayerStatistic> save(final UnitOfWork unitOfWork,
                final PlayerStatistic value, final RecordRevision expectedRevision) {
            expected = expectedRevision;
            saved = value;
            current = value;
            return saveFailure ? Result.<PlayerStatistic>failure(FAILURE) : Result.success(value);
        }
    }

    private abstract static class AggregateStore {
        boolean claimed = true;
        boolean claimFailure;
        boolean saveFailure;

        final Result<Boolean> claimResult() {
            return claimFailure ? Result.<Boolean>failure(FAILURE) : Result.success(claimed);
        }
    }

    private static final class MatchStore extends AggregateStore
            implements MatchStatisticsProjection.Store {
        @Override
        public Result<Boolean> claim(final UnitOfWork unit, final IdempotencyKey key,
                                     final Instant occurredAt) {
            return claimResult();
        }

        @Override
        public Result<MatchStatistic> save(final UnitOfWork unit, final MatchStatistic value,
                                           final RecordRevision expected) {
            return saveFailure ? Result.<MatchStatistic>failure(FAILURE) : Result.success(value);
        }
    }

    private static final class TeamStore extends AggregateStore
            implements TeamStatisticsProjection.Store {
        @Override
        public Result<Boolean> claim(final UnitOfWork unit, final IdempotencyKey key,
                                     final Instant occurredAt) {
            return claimResult();
        }

        @Override
        public Result<TeamStatistic> save(final UnitOfWork unit, final TeamStatistic value,
                                          final RecordRevision expected) {
            return saveFailure ? Result.<TeamStatistic>failure(FAILURE) : Result.success(value);
        }
    }

    private static final class SeasonStore extends AggregateStore
            implements SeasonalStatisticsProjection.Store {
        @Override
        public Result<Boolean> claim(final UnitOfWork unit, final IdempotencyKey key,
                                     final Instant occurredAt) {
            return claimResult();
        }

        @Override
        public Result<SeasonalStatistic> save(final UnitOfWork unit,
                final SeasonalStatistic value, final RecordRevision expected) {
            return saveFailure ? Result.<SeasonalStatistic>failure(FAILURE)
                    : Result.success(value);
        }
    }

    private static final class MemoryUnit implements UnitOfWork {
        @Override public State state() { return State.ACTIVE; }
        @Override public Result<State> commit() { return Result.success(State.COMMITTED); }
        @Override public Result<State> rollback() { return Result.success(State.ROLLED_BACK); }
        @Override public void close() { }
    }
}
