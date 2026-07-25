package io.zartra.bedwars.statistics.model;

import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.storage.api.RecordRevision;
import java.util.Objects;

/** Immutable match-scoped statistic input or persisted snapshot. */
public final class MatchStatistic {
    private final MatchId matchId;
    private final StatisticId statisticId;
    private final long value;
    private final RecordRevision revision;
    private final StatisticAudit audit;
    /** Creates a non-negative match value. */
    public MatchStatistic(final MatchId matchId, final StatisticId statisticId, final long value, final StatisticAudit audit) {
        this(matchId, statisticId, value, RecordRevision.initial(), audit);
    }
    /** Creates a non-negative match value with an explicit optimistic revision. */
    public MatchStatistic(final MatchId matchId, final StatisticId statisticId, final long value,
                          final RecordRevision revision, final StatisticAudit audit) {
        this.matchId = Objects.requireNonNull(matchId, "matchId");
        this.statisticId = Objects.requireNonNull(statisticId, "statisticId");
        if (value < 0) { throw new IllegalArgumentException("value must be non-negative"); }
        this.value = value;
        this.revision = Objects.requireNonNull(revision, "revision");
        this.audit = Objects.requireNonNull(audit, "audit");
    }
    /** @return match owner */ public MatchId matchId() { return matchId; }
    /** @return measured statistic */ public StatisticId statisticId() { return statisticId; }
    /** @return deterministic value */ public long value() { return value; }
    /** @return optimistic persistence revision */ public RecordRevision revision() { return revision; }
    /** @return audit metadata */ public StatisticAudit audit() { return audit; }
}
