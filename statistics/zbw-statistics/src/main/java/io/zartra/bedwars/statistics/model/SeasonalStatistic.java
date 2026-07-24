package io.zartra.bedwars.statistics.model;

import io.zartra.bedwars.api.identity.PlayerId;
import java.util.Objects;

/** Immutable player statistic segregated by one configured season. */
public final class SeasonalStatistic {
    private final PlayerId playerId; private final StatisticId statisticId; private final StatisticScope season; private final long value; private final StatisticAudit audit;
    /** Creates a non-negative seasonal aggregate. */
    public SeasonalStatistic(final PlayerId playerId, final StatisticId statisticId, final StatisticScope season, final long value, final StatisticAudit audit) {
        this.playerId = Objects.requireNonNull(playerId, "playerId"); this.statisticId = Objects.requireNonNull(statisticId, "statisticId"); this.season = Objects.requireNonNull(season, "season");
        if (value < 0) { throw new IllegalArgumentException("value must be non-negative"); }
        this.value = value; this.audit = Objects.requireNonNull(audit, "audit");
    }
    /** @return aggregate owner */ public PlayerId playerId() { return playerId; }
    /** @return measured statistic */ public StatisticId statisticId() { return statisticId; }
    /** @return configured season */ public StatisticScope season() { return season; }
    /** @return deterministic value */ public long value() { return value; }
    /** @return audit metadata */ public StatisticAudit audit() { return audit; }
}
