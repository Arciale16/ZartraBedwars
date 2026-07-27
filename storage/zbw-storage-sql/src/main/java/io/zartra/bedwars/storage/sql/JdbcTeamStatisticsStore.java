package io.zartra.bedwars.storage.sql;

import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.statistics.model.StatisticAudit;
import io.zartra.bedwars.statistics.model.StatisticId;
import io.zartra.bedwars.statistics.model.TeamStatistic;
import io.zartra.bedwars.statistics.runtime.TeamStatisticsProjection;
import io.zartra.bedwars.storage.api.RecordRevision;
import io.zartra.bedwars.storage.api.UnitOfWork;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;

/** Prepared-statement team aggregate store using only caller-owned JDBC transactions. */
public final class JdbcTeamStatisticsStore implements TeamStatisticsProjection.Store {
    private final int timeoutSeconds;

    /** Creates a store with a positive query timeout. */
    public JdbcTeamStatisticsStore(final int timeoutSeconds) {
        if (timeoutSeconds < 1) {
            throw new IllegalArgumentException("timeoutSeconds must be positive");
        }
        this.timeoutSeconds = timeoutSeconds;
    }

    /** Loads an aggregate from the caller-owned transaction. */
    public Result<Optional<TeamStatistic>> find(final UnitOfWork unitOfWork,
                                                final MatchId matchId,
                                                final DefinitionId teamId,
                                                final StatisticId statisticId) {
        final JdbcUnitOfWork unit = JdbcSupport.require(unitOfWork, false);
        try (PreparedStatement statement = prepare(unit,
                "SELECT value,revision,actor,correlation_id,recorded_at "
                        + "FROM statistics_team_aggregates "
                        + "WHERE match_id=? AND team_id=? AND statistic_id=?")) {
            statement.setString(1, matchId.toString());
            statement.setString(2, teamId.toString());
            statement.setString(3, statisticId.toString());
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) {
                    return Result.success(Optional.empty());
                }
                final StatisticAudit audit = new StatisticAudit(rows.getString(3),
                        CorrelationId.parse(rows.getString(4)),
                        Instant.ofEpochMilli(rows.getLong(5)));
                return Result.success(Optional.of(new TeamStatistic(matchId,
                        DefinitionId.parse(teamId.toString()),
                        statisticId, rows.getLong(1), RecordRevision.of(rows.getLong(2)),
                        audit)));
            }
        } catch (SQLException failure) {
            return Result.failure(SqlErrors.classify(failure));
        }
    }

    @Override
    public Result<Boolean> claim(final UnitOfWork unitOfWork, final IdempotencyKey key,
                                 final Instant occurredAt) {
        final JdbcUnitOfWork unit = JdbcSupport.require(unitOfWork, true);
        try (PreparedStatement lookup = prepare(unit,
                "SELECT 1 FROM statistics_event_claims WHERE idempotency_key=?")) {
            lookup.setString(1, key.toString());
            try (ResultSet rows = lookup.executeQuery()) {
                if (rows.next()) {
                    return Result.success(false);
                }
            }
            try (PreparedStatement insert = prepare(unit,
                    "INSERT INTO statistics_event_claims(idempotency_key,claimed_at) VALUES(?,?)")) {
                insert.setString(1, key.toString());
                insert.setLong(2, occurredAt.toEpochMilli());
                insert.executeUpdate();
                return Result.success(true);
            }
        } catch (SQLException failure) {
            if (SqlErrors.duplicate(failure)) {
                return Result.success(false);
            }
            return Result.failure(SqlErrors.classify(failure));
        }
    }

    @Override
    public Result<TeamStatistic> save(final UnitOfWork unitOfWork,
                                      final TeamStatistic value,
                                      final RecordRevision expected) {
        final JdbcUnitOfWork unit = JdbcSupport.require(unitOfWork, true);
        if (!value.revision().equals(expected.next())) {
            throw new IllegalArgumentException("saved revision must follow expected revision");
        }
        try {
            final int changed = expected.value() == 0
                    ? insert(unit, value) : update(unit, value, expected);
            return changed == 1 ? Result.success(value) : Result.failure(SqlErrors.CONFLICT);
        } catch (SQLException failure) {
            return Result.failure(SqlErrors.classify(failure));
        }
    }

    private int insert(final JdbcUnitOfWork unit, final TeamStatistic value)
            throws SQLException {
        try (PreparedStatement statement = prepare(unit,
                "INSERT INTO statistics_team_aggregates(match_id,team_id,statistic_id,value,"
                        + "revision,actor,correlation_id,recorded_at) VALUES(?,?,?,?,?,?,?,?)")) {
            bindValue(statement, value);
            return statement.executeUpdate();
        }
    }

    private int update(final JdbcUnitOfWork unit, final TeamStatistic value,
                       final RecordRevision expected) throws SQLException {
        try (PreparedStatement statement = prepare(unit,
                "UPDATE statistics_team_aggregates SET value=?,revision=?,actor=?,"
                        + "correlation_id=?,recorded_at=? WHERE match_id=? AND team_id=? "
                        + "AND statistic_id=? AND revision=?")) {
            statement.setLong(1, value.value());
            statement.setLong(2, value.revision().value());
            statement.setString(3, value.audit().actor());
            statement.setString(4, value.audit().correlationId().toString());
            statement.setLong(5, value.audit().recordedAt().toEpochMilli());
            statement.setString(6, value.matchId().toString());
            statement.setString(7, value.teamId().toString());
            statement.setString(8, value.statisticId().toString());
            statement.setLong(9, expected.value());
            return statement.executeUpdate();
        }
    }

    private PreparedStatement prepare(final JdbcUnitOfWork unit, final String sql)
            throws SQLException {
        final PreparedStatement statement = unit.connection().prepareStatement(sql);
        statement.setQueryTimeout(timeoutSeconds);
        return statement;
    }

    private static void bindValue(final PreparedStatement statement, final TeamStatistic value)
            throws SQLException {
        statement.setString(1, value.matchId().toString());
        statement.setString(2, value.teamId().toString());
        statement.setString(3, value.statisticId().toString());
        statement.setLong(4, value.value());
        statement.setLong(5, value.revision().value());
        statement.setString(6, value.audit().actor());
        statement.setString(7, value.audit().correlationId().toString());
        statement.setLong(8, value.audit().recordedAt().toEpochMilli());
    }
}
