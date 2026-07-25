package io.zartra.bedwars.statistics.repository;

import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.statistics.model.MatchStatistic;
import io.zartra.bedwars.statistics.model.PlayerStatistic;
import io.zartra.bedwars.statistics.model.SeasonalStatistic;
import io.zartra.bedwars.statistics.model.StatisticDefinition;
import io.zartra.bedwars.statistics.model.StatisticId;
import io.zartra.bedwars.statistics.model.StatisticScope;
import io.zartra.bedwars.statistics.model.TeamStatistic;
import io.zartra.bedwars.storage.api.RecordRevision;
import io.zartra.bedwars.storage.api.UnitOfWork;
import java.util.Optional;

/** JDBC-free ports;
each method runs in a caller-owned transaction off a server owner thread. */
public final class StatisticRepositories {
    private StatisticRepositories() { }
    /** Versioned statistic definition port. */ public interface Definitions { /** @return definition when present */ Result<Optional<StatisticDefinition>> find(UnitOfWork unitOfWork, StatisticId id);
    /** @return saved definition */ Result<StatisticDefinition> save(UnitOfWork unitOfWork, StatisticDefinition definition);
    }
    /** Player aggregate port with explicit optimistic revision. */ public interface Players { /** @return aggregate when present */ Result<Optional<PlayerStatistic>> find(UnitOfWork unitOfWork, PlayerId playerId, StatisticId statisticId, StatisticScope scope);
    /** @return saved aggregate */ Result<PlayerStatistic> save(UnitOfWork unitOfWork, PlayerStatistic statistic, RecordRevision expectedRevision);
    }
    /** Match aggregate port. */ public interface Matches { /** @return match statistic when present */ Result<Optional<MatchStatistic>> find(UnitOfWork unitOfWork, MatchId matchId, StatisticId statisticId);
    /** @return saved statistic */ Result<MatchStatistic> save(UnitOfWork unitOfWork, MatchStatistic statistic);
    }
    /** Team aggregate port. */ public interface Teams { /** @return saved team statistic */ Result<TeamStatistic> save(UnitOfWork unitOfWork, TeamStatistic statistic);
    }
    /** Season aggregate port. */ public interface Seasons { /** @return saved seasonal statistic */ Result<SeasonalStatistic> save(UnitOfWork unitOfWork, SeasonalStatistic statistic);
    }
}
