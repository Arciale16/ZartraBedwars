package io.zartra.bedwars.statistics.model;

import io.zartra.bedwars.api.identity.MatchId;
import java.util.Objects;

/** Immutable match-scoped statistic input or persisted snapshot. */
public final class MatchStatistic {
    private final MatchId matchId; private final StatisticId statisticId; private final long value; private final StatisticAudit audit;
    /** Creates a non-negative match value. */
    public MatchStatistic(final MatchId matchId, final StatisticId statisticId, final long value, final StatisticAudit audit) {
        this.matchId = Objects.requireNonNull(matchId, "matchId"); this.statisticId = Objects.requireNonNull(statisticId, "statisticId");
        if (value < 0) { throw new IllegalArgumentException("value must be non-negative"); }
        this.value = value; this.audit = Objects.requireNonNull(audit, "audit");
    }
    /** @return match owner */ public MatchId matchId() { return matchId; }
    /** @return measured statistic */ public StatisticId statisticId() { return statisticId; }
    /** @return deterministic value */ public long value() { return value; }
    /** @return audit metadata */ public StatisticAudit audit() { return audit; }
}
