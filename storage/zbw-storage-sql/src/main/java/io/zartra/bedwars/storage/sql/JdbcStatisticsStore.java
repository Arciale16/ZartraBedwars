package io.zartra.bedwars.storage.sql;

import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.statistics.model.PlayerStatistic;
import io.zartra.bedwars.statistics.model.StatisticAudit;
import io.zartra.bedwars.statistics.model.StatisticId;
import io.zartra.bedwars.statistics.model.StatisticScope;
import io.zartra.bedwars.statistics.runtime.StatisticsProjectionEngine;
import io.zartra.bedwars.storage.api.RecordRevision;
import io.zartra.bedwars.storage.api.UnitOfWork;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;

/** Bounded prepared-statement M15 aggregate store with durable duplicate suppression. */
public final class JdbcStatisticsStore implements StatisticsProjectionEngine.Store {
    private final int timeoutSeconds;
    /** Creates a store with a positive statement timeout. */ public JdbcStatisticsStore(final int timeoutSeconds) { if (timeoutSeconds < 1) { throw new IllegalArgumentException("timeoutSeconds must be positive"); } this.timeoutSeconds = timeoutSeconds; }
    @Override public Result<Boolean> claim(final UnitOfWork unitOfWork, final IdempotencyKey key, final Instant now) {
        final JdbcUnitOfWork unit = JdbcSupport.require(unitOfWork, true);
        try (PreparedStatement lookup = prepare(unit, "SELECT 1 FROM statistics_event_claims WHERE idempotency_key=?")) {
            lookup.setString(1, key.toString()); try (ResultSet rows = lookup.executeQuery()) { if (rows.next()) { return Result.success(false); } }
            try (PreparedStatement insert = prepare(unit, "INSERT INTO statistics_event_claims(idempotency_key,claimed_at) VALUES(?,?)")) { insert.setString(1, key.toString()); insert.setLong(2, now.toEpochMilli()); insert.executeUpdate(); return Result.success(true); }
        } catch (SQLException failure) { return Result.failure(SqlErrors.classify(failure)); }
    }
    @Override public Result<Optional<PlayerStatistic>> find(final UnitOfWork unitOfWork, final PlayerId playerId, final StatisticId statisticId, final StatisticScope scope) {
        final JdbcUnitOfWork unit = JdbcSupport.require(unitOfWork, false);
        try (PreparedStatement statement = prepare(unit, "SELECT value,revision,actor,correlation_id,recorded_at FROM statistics_player_aggregates WHERE player_id=? AND statistic_id=? AND scope_id=?")) {
            bindKey(statement, playerId, statisticId, scope); try (ResultSet rows = statement.executeQuery()) { if (!rows.next()) { return Result.success(Optional.empty()); } final StatisticAudit audit = new StatisticAudit(rows.getString(3), CorrelationId.parse(rows.getString(4)), Instant.ofEpochMilli(rows.getLong(5))); return Result.success(Optional.of(new PlayerStatistic(playerId, statisticId, scope, rows.getLong(1), RecordRevision.of(rows.getLong(2)), audit))); }
        } catch (SQLException failure) { return Result.failure(SqlErrors.classify(failure)); }
    }
    @Override public Result<PlayerStatistic> save(final UnitOfWork unitOfWork, final PlayerStatistic value, final RecordRevision expected) {
        final JdbcUnitOfWork unit = JdbcSupport.require(unitOfWork, true);
        try {
            final int changed;
            if (expected.value() == 0) {
                try (PreparedStatement statement = prepare(unit, "INSERT INTO statistics_player_aggregates(player_id,statistic_id,scope_id,value,revision,actor,correlation_id,recorded_at) VALUES(?,?,?,?,?,?,?,?)")) { bind(statement, value); changed = statement.executeUpdate(); }
            } else {
                try (PreparedStatement statement = prepare(unit, "UPDATE statistics_player_aggregates SET value=?,revision=?,actor=?,correlation_id=?,recorded_at=? WHERE player_id=? AND statistic_id=? AND scope_id=? AND revision=?")) { statement.setLong(1, value.value()); statement.setLong(2, value.revision().value()); statement.setString(3, value.audit().actor()); statement.setString(4, value.audit().correlationId().toString()); statement.setLong(5, value.audit().recordedAt().toEpochMilli()); statement.setString(6, value.playerId().toString()); statement.setString(7, value.statisticId().toString()); statement.setString(8, value.scope().toString()); statement.setLong(9, expected.value()); changed = statement.executeUpdate(); }
            }
            return changed == 1 ? Result.success(value) : Result.failure(SqlErrors.CONFLICT);
        } catch (SQLException failure) { return Result.failure(SqlErrors.classify(failure)); }
    }
    private PreparedStatement prepare(final JdbcUnitOfWork unit, final String sql) throws SQLException { final PreparedStatement statement = unit.connection().prepareStatement(sql); statement.setQueryTimeout(timeoutSeconds); return statement; }
    private static void bindKey(final PreparedStatement statement, final PlayerId playerId, final StatisticId statisticId, final StatisticScope scope) throws SQLException { statement.setString(1, playerId.toString()); statement.setString(2, statisticId.toString()); statement.setString(3, scope.toString()); }
    private static void bind(final PreparedStatement statement, final PlayerStatistic value) throws SQLException { statement.setString(1, value.playerId().toString()); statement.setString(2, value.statisticId().toString()); statement.setString(3, value.scope().toString()); statement.setLong(4, value.value()); statement.setLong(5, value.revision().value()); statement.setString(6, value.audit().actor()); statement.setString(7, value.audit().correlationId().toString()); statement.setLong(8, value.audit().recordedAt().toEpochMilli()); }
}
