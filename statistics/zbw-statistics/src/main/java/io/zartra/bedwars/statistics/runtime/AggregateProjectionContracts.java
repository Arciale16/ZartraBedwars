package io.zartra.bedwars.statistics.runtime;

import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.api.identity.NamespacedIdentifier;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.statistics.model.MatchStatistic;
import io.zartra.bedwars.statistics.model.SeasonalStatistic;
import io.zartra.bedwars.statistics.model.TeamStatistic;
import io.zartra.bedwars.storage.api.RecordRevision;
import io.zartra.bedwars.storage.api.UnitOfWork;
import java.time.Instant;

/** Caller-owned transaction contracts for M15 match, team and season aggregates. */
public final class AggregateProjectionContracts {
    private AggregateProjectionContracts() { }
    /** Projects existing M08 facts into match aggregates. */ public interface MatchProjector { /** @return saved match aggregate */ Result<MatchStatistic> project(UnitOfWork unit, MatchStatistic value, RecordRevision expected, IdempotencyKey key, Instant occurredAt);
    }
    /** Projects existing M08 facts into arena-defined team aggregates. */ public interface TeamProjector { /** @return saved team aggregate */ Result<TeamStatistic> project(UnitOfWork unit, TeamStatistic value, RecordRevision expected, IdempotencyKey key, Instant occurredAt);
    }
    /** Projects a player fact into one explicit non-overlapping season. */ public interface SeasonProjector { /** @return saved seasonal aggregate */ Result<SeasonalStatistic> project(UnitOfWork unit, SeasonalStatistic value, RecordRevision expected, IdempotencyKey key, Instant occurredAt);
    }
    /** Validates deterministic season rollover boundaries. */ public interface SeasonBoundary { /** @return true when timestamp belongs to this season */ boolean contains(Instant timestamp);
    /** @return canonical season identity */ NamespacedIdentifier seasonId();
    }
    /** Rebuild work is explicit, bounded and runs outside Minecraft owner threads. */ public interface Rebuilder { /** @return number of deterministic aggregates rebuilt */ Result<Integer> rebuild(UnitOfWork unit, MatchId matchId, int maximumEvents);
    }
}
