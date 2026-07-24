package io.zartra.bedwars.statistics.model;

import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.api.identity.NamespacedIdentifier;
import java.util.Objects;

/** Immutable team statistic using an arena-defined identity rather than a fixed index. */
public final class TeamStatistic {
    private final MatchId matchId; private final NamespacedIdentifier teamId; private final StatisticId statisticId; private final long value; private final StatisticAudit audit;
    /** Creates a non-negative team statistic snapshot. */
    public TeamStatistic(final MatchId matchId, final NamespacedIdentifier teamId, final StatisticId statisticId, final long value, final StatisticAudit audit) {
        this.matchId = Objects.requireNonNull(matchId, "matchId"); this.teamId = Objects.requireNonNull(teamId, "teamId"); this.statisticId = Objects.requireNonNull(statisticId, "statisticId");
        if (value < 0) { throw new IllegalArgumentException("value must be non-negative"); }
        this.value = value; this.audit = Objects.requireNonNull(audit, "audit");
    }
    /** @return match owner */ public MatchId matchId() { return matchId; }
    /** @return arena-defined team identity */ public NamespacedIdentifier teamId() { return teamId; }
    /** @return measured statistic */ public StatisticId statisticId() { return statisticId; }
    /** @return deterministic value */ public long value() { return value; }
    /** @return audit metadata */ public StatisticAudit audit() { return audit; }
}
