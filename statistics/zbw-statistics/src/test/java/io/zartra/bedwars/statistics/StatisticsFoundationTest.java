package io.zartra.bedwars.statistics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.statistics.leaderboard.LeaderboardContracts;
import io.zartra.bedwars.statistics.model.StatisticAudit;
import io.zartra.bedwars.statistics.model.StatisticCategory;
import io.zartra.bedwars.statistics.model.StatisticDefinition;
import io.zartra.bedwars.statistics.model.StatisticId;
import io.zartra.bedwars.statistics.model.StatisticScope;
import io.zartra.bedwars.statistics.projection.StatisticProjection;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Unit tests for M15 immutable contracts and deterministic ordering. */
final class StatisticsFoundationTest {
    private static final StatisticAudit AUDIT = new StatisticAudit("m15-test",
            CorrelationId.of(UUID.fromString("00000000-0000-0000-0000-000000000001")), Instant.EPOCH);

    @Test
    void definitionsAreVersionedAndValidated() {
        final StatisticDefinition definition = new StatisticDefinition(StatisticId.of("zartra", "wins"),
                StatisticCategory.MATCH, 1, StatisticDefinition.Aggregation.SUM, AUDIT);
        assertEquals(1, definition.version());
        assertThrows(IllegalArgumentException.class, () -> new StatisticDefinition(
                StatisticId.of("zartra", "wins"), StatisticCategory.MATCH, 0,
                StatisticDefinition.Aggregation.SUM, AUDIT));
    }

    @Test
    void projectionBoundsAndIdempotencyInputsFailClosed() {
        assertThrows(IllegalArgumentException.class, () -> new StatisticProjection.RebuildRequest(
                StatisticId.of("zartra", "wins"), StatisticScope.of("global", "all"), 0));
        assertThrows(IllegalArgumentException.class, () -> new StatisticProjection.RebuildRequest(
                StatisticId.of("zartra", "wins"), StatisticScope.of("global", "all"), 100001));
    }

    @Test
    void leaderboardTieOrderingIsDeterministicAndPagesAreBounded() {
        final PlayerId first = PlayerId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        final PlayerId second = PlayerId.of(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        final LeaderboardContracts.Entry high = new LeaderboardContracts.Entry(second, 9, 1);
        final LeaderboardContracts.Entry low = new LeaderboardContracts.Entry(first, 9, 2);
        final java.util.List<LeaderboardContracts.Entry> entries = Arrays.asList(low, high);
        entries.sort(LeaderboardContracts.comparator(LeaderboardContracts.Sort.DESCENDING));
        assertEquals(first, entries.get(0).playerId());
        assertThrows(IllegalArgumentException.class, () -> new LeaderboardContracts.Page(entries, 0, 1, false));
    }
}
