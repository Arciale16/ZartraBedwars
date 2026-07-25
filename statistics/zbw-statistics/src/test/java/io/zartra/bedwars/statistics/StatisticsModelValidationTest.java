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
import io.zartra.bedwars.statistics.leaderboard.LeaderboardContracts;
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
import io.zartra.bedwars.storage.api.RecordRevision;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Boundary coverage for immutable M15 statistics and leaderboard contracts. */
final class StatisticsModelValidationTest {
    private static final Instant NOW = Instant.parse("2026-07-25T10:00:00Z");
    private static final PlayerId PLAYER = PlayerId.of(new UUID(0, 401));
    private static final MatchId MATCH = MatchId.of(new UUID(0, 402));
    private static final DefinitionId TEAM = DefinitionId.of("zartra", "team/blue");
    private static final StatisticId KILLS = StatisticId.of("zartra", "kills");
    private static final StatisticScope GLOBAL = StatisticScope.of("zartra", "global");
    private static final StatisticScope SEASON = StatisticScope.of("zartra", "season/one");
    private static final StatisticAudit AUDIT = new StatisticAudit("statistics-model-test",
            CorrelationId.of(new UUID(0, 403)), NOW);

    @Test
    void aggregateModelsExposeTypedStateAndRejectNegativeValues() {
        final MatchStatistic match = new MatchStatistic(MATCH, KILLS, 2,
                RecordRevision.of(3), AUDIT);
        assertEquals(MATCH, match.matchId());
        assertEquals(KILLS, match.statisticId());
        assertEquals(2L, match.value());
        assertEquals(3L, match.revision().value());
        assertEquals(AUDIT, match.audit());

        final TeamStatistic team = new TeamStatistic(MATCH, TEAM, KILLS, 4,
                RecordRevision.of(5), AUDIT);
        assertEquals(MATCH, team.matchId());
        assertEquals(TEAM, team.teamId());
        assertEquals(KILLS, team.statisticId());
        assertEquals(4L, team.value());
        assertEquals(5L, team.revision().value());
        assertEquals(AUDIT, team.audit());

        final SeasonalStatistic seasonal = new SeasonalStatistic(PLAYER, KILLS, SEASON, 6,
                RecordRevision.of(7), AUDIT);
        assertEquals(PLAYER, seasonal.playerId());
        assertEquals(KILLS, seasonal.statisticId());
        assertEquals(SEASON, seasonal.season());
        assertEquals(6L, seasonal.value());
        assertEquals(7L, seasonal.revision().value());
        assertEquals(AUDIT, seasonal.audit());

        final PlayerStatistic player = new PlayerStatistic(PLAYER, KILLS, GLOBAL, 8,
                RecordRevision.of(9), AUDIT);
        assertEquals(PLAYER, player.playerId());
        assertEquals(KILLS, player.statisticId());
        assertEquals(GLOBAL, player.scope());
        assertEquals(8L, player.value());
        assertEquals(9L, player.revision().value());
        assertEquals(AUDIT, player.audit());

        assertThrows(IllegalArgumentException.class,
                () -> new MatchStatistic(MATCH, KILLS, -1, AUDIT));
        assertThrows(IllegalArgumentException.class,
                () -> new TeamStatistic(MATCH, TEAM, KILLS, -1, AUDIT));
        assertThrows(IllegalArgumentException.class,
                () -> new SeasonalStatistic(PLAYER, KILLS, SEASON, -1, AUDIT));
        assertThrows(IllegalArgumentException.class,
                () -> new PlayerStatistic(PLAYER, KILLS, GLOBAL, -1,
                        RecordRevision.initial(), AUDIT));
    }

    @Test
    void definitionsAuditAndProjectionValuesValidateEveryBoundary() {
        final StatisticDefinition definition = new StatisticDefinition(KILLS,
                StatisticCategory.COMBAT, 2, StatisticDefinition.Aggregation.MAXIMUM, AUDIT);
        assertEquals(KILLS, definition.id());
        assertEquals(StatisticCategory.COMBAT, definition.category());
        assertEquals(2, definition.version());
        assertEquals(StatisticDefinition.Aggregation.MAXIMUM, definition.aggregation());
        assertEquals(AUDIT, definition.audit());
        assertEquals("statistics-model-test", AUDIT.actor());
        assertEquals(NOW, AUDIT.recordedAt());

        assertThrows(IllegalArgumentException.class,
                () -> new StatisticAudit(null, CorrelationId.of(new UUID(0, 1)), NOW));
        assertThrows(IllegalArgumentException.class,
                () -> new StatisticAudit(" ", CorrelationId.of(new UUID(0, 1)), NOW));
        assertThrows(IllegalArgumentException.class,
                () -> new StatisticAudit(repeat('a', 129),
                        CorrelationId.of(new UUID(0, 1)), NOW));

        final StatisticProjection.Event event = new StatisticProjection.Event(
                EventId.of(new UUID(0, 404)), IdempotencyKey.of("test", "projection"),
                StatisticProjection.Source.PROGRESSION, KILLS, GLOBAL, 0, AUDIT);
        assertEquals(StatisticProjection.Source.PROGRESSION, event.source());
        assertEquals(0L, event.delta());
        assertEquals(AUDIT, event.audit());
        assertThrows(IllegalArgumentException.class,
                () -> new StatisticProjection.Event(EventId.of(new UUID(0, 405)),
                        IdempotencyKey.of("test", "negative"),
                        StatisticProjection.Source.MATCH, KILLS, GLOBAL, -1, AUDIT));

        final StatisticProjection.ResultState state = new StatisticProjection.ResultState(
                StatisticProjection.Status.RETRYABLE_FAILURE, NOW);
        assertEquals(StatisticProjection.Status.RETRYABLE_FAILURE, state.status());
        assertEquals(NOW, state.processedAt());
        final StatisticProjection.RebuildRequest rebuild = new StatisticProjection.RebuildRequest(
                KILLS, GLOBAL, 100000);
        assertEquals(KILLS, rebuild.statisticId());
        assertEquals(GLOBAL, rebuild.scope());
        assertEquals(100000, rebuild.maximumEvents());
    }

    @Test
    void legacyLeaderboardContractsCoverAscendingAndValidationBranches() {
        final LeaderboardContracts.Definition definition = new LeaderboardContracts.Definition(
                KILLS, GLOBAL, LeaderboardContracts.Sort.ASCENDING);
        assertEquals(KILLS, definition.statisticId());
        assertEquals(GLOBAL, definition.scope());
        assertEquals(LeaderboardContracts.Sort.ASCENDING, definition.sort());

        final PlayerId first = PlayerId.of(new UUID(0, 410));
        final PlayerId second = PlayerId.of(new UUID(0, 411));
        final LeaderboardContracts.Entry high = new LeaderboardContracts.Entry(second, 9, 2);
        final LeaderboardContracts.Entry low = new LeaderboardContracts.Entry(first, 2, 1);
        final java.util.List<LeaderboardContracts.Entry> entries =
                new java.util.ArrayList<LeaderboardContracts.Entry>(Arrays.asList(high, low));
        entries.sort(LeaderboardContracts.comparator(LeaderboardContracts.Sort.ASCENDING));
        assertEquals(low, entries.get(0));
        assertEquals(second, high.playerId());
        assertEquals(9L, high.value());
        assertEquals(2, high.rank());

        final LeaderboardContracts.Page page = new LeaderboardContracts.Page(entries, 0, 2, true);
        assertEquals(2, page.entries().size());
        assertEquals(0, page.offset());
        assertEquals(2, page.pageSize());
        assertTrue(page.hasNext());
        assertThrows(UnsupportedOperationException.class,
                () -> page.entries().add(low));
        assertThrows(IllegalArgumentException.class,
                () -> new LeaderboardContracts.Entry(first, -1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new LeaderboardContracts.Entry(first, 1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new LeaderboardContracts.Page(Collections.singletonList(low), -1, 1, false));
        assertThrows(IllegalArgumentException.class,
                () -> new LeaderboardContracts.Page(Collections.singletonList(low), 0, 0, false));
        assertFalse(new LeaderboardContracts.Page(Collections.<LeaderboardContracts.Entry>emptyList(),
                0, 100, false).hasNext());
    }

    private static String repeat(final char value, final int count) {
        final char[] characters = new char[count];
        Arrays.fill(characters, value);
        return new String(characters);
    }
}
