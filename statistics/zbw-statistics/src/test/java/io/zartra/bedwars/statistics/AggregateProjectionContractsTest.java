package io.zartra.bedwars.statistics;

import static org.junit.jupiter.api.Assertions.assertThrows;
import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.statistics.model.MatchStatistic;
import io.zartra.bedwars.statistics.model.StatisticAudit;
import io.zartra.bedwars.statistics.model.StatisticId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** M15 Phase 2B-1 aggregate boundary validation. */
final class AggregateProjectionContractsTest {
    @Test void rejectsNegativeMatchAggregateValues() {
        final StatisticAudit audit = new StatisticAudit("test", CorrelationId.random(), Instant.EPOCH);
        assertThrows(IllegalArgumentException.class, () -> new MatchStatistic(MatchId.random(),
                StatisticId.of("zartra", "wins"), -1L, audit));
    }
}
