package io.zartra.bedwars.statistics.model;

import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.storage.api.RecordRevision;
import java.util.Objects;

/** Immutable player aggregate within exactly one statistic scope. */
public final class PlayerStatistic {
    private final PlayerId playerId;
    private final StatisticId statisticId;
    private final StatisticScope scope;
    private final long value;
    private final RecordRevision revision;
    private final StatisticAudit audit;
    /** Creates a non-negative aggregate snapshot. */
    public PlayerStatistic(final PlayerId playerId, final StatisticId statisticId, final StatisticScope scope,
                           final long value, final RecordRevision revision, final StatisticAudit audit) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.statisticId = Objects.requireNonNull(statisticId, "statisticId");
        this.scope = Objects.requireNonNull(scope, "scope");
        if (value < 0) { throw new IllegalArgumentException("value must be non-negative");
        }
        this.value = value;
        this.revision = Objects.requireNonNull(revision, "revision");
        this.audit = Objects.requireNonNull(audit, "audit");
    }
    /** @return aggregate owner */ public PlayerId playerId() { return playerId;
    }
    /** @return measured statistic */ public StatisticId statisticId() { return statisticId;
    }
    /** @return isolated aggregate dimension */ public StatisticScope scope() { return scope;
    }
    /** @return deterministic numeric value */ public long value() { return value;
    }
    /** @return optimistic revision */ public RecordRevision revision() { return revision;
    }
    /** @return audit metadata */ public StatisticAudit audit() { return audit;
    }
}
