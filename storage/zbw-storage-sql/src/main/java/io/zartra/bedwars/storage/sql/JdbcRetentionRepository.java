package io.zartra.bedwars.storage.sql;

import io.zartra.bedwars.api.identity.CaseId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.storage.api.RecordKey;
import io.zartra.bedwars.storage.api.RetentionPolicy;
import io.zartra.bedwars.storage.api.RetentionRepository;
import io.zartra.bedwars.storage.api.UnitOfWork;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;

/** Prepared-statement retention, legal-hold and tombstone repository. */
final class JdbcRetentionRepository implements RetentionRepository {
    @Override public Result<Boolean> retain(final UnitOfWork unitOfWork, final RecordKey key,
                                            final RetentionPolicy policy, final Instant expiresAt) {
        if (key == null || policy == null || expiresAt == null) { throw new NullPointerException("retention argument"); }
        final JdbcUnitOfWork jdbc = JdbcSupport.require(unitOfWork, true);
        return JdbcSupport.execute(jdbc, new JdbcSupport.SqlOperation<Boolean>() {
            @Override public Result<Boolean> execute() throws SQLException {
                try (PreparedStatement delete = jdbc.connection().prepareStatement(
                        "DELETE FROM zbw_retention WHERE aggregate_type = ? AND aggregate_id = ?")) {
                    delete.setQueryTimeout(JdbcSupport.timeoutSeconds(jdbc));
                    bindKey(delete, key);
                    delete.executeUpdate();
                }
                try (PreparedStatement insert = jdbc.connection().prepareStatement(
                        "INSERT INTO zbw_retention(aggregate_type, aggregate_id, retention_class, "
                                + "expires_at, deletion_deadline_ms) VALUES(?, ?, ?, ?, ?)")) {
                    insert.setQueryTimeout(JdbcSupport.timeoutSeconds(jdbc));
                    bindKey(insert, key);
                    insert.setString(3, policy.retentionClass().toString());
                    insert.setLong(4, expiresAt.toEpochMilli());
                    insert.setLong(5, policy.deletionDeadline().toMillis());
                    return Result.success(insert.executeUpdate() == 1);
                }
            }
        });
    }

    @Override public Result<Boolean> hold(final UnitOfWork unitOfWork, final CaseId caseId,
                                          final RecordKey key, final PlayerId authorizedBy,
                                          final Instant placedAt) {
        if (caseId == null || key == null || authorizedBy == null || placedAt == null) {
            throw new NullPointerException("hold argument");
        }
        final JdbcUnitOfWork jdbc = JdbcSupport.require(unitOfWork, true);
        return JdbcSupport.execute(jdbc, new JdbcSupport.SqlOperation<Boolean>() {
            @Override public Result<Boolean> execute() throws SQLException {
                try (PreparedStatement statement = jdbc.connection().prepareStatement(
                        "INSERT INTO zbw_legal_hold(case_id, aggregate_type, aggregate_id, "
                                + "authorized_by, placed_at, released_at, released_by) VALUES(?, ?, ?, ?, ?, NULL, NULL)")) {
                    statement.setQueryTimeout(JdbcSupport.timeoutSeconds(jdbc));
                    statement.setString(1, caseId.toString());
                    statement.setString(2, key.aggregateType().toString());
                    statement.setString(3, key.aggregateId().toString());
                    statement.setString(4, authorizedBy.toString());
                    statement.setLong(5, placedAt.toEpochMilli());
                    try { return Result.success(statement.executeUpdate() == 1); }
                    catch (SQLException exception) {
                        if (SqlErrors.duplicate(exception)) { return Result.success(false); }
                        throw exception;
                    }
                }
            }
        });
    }

    @Override public Result<Boolean> release(final UnitOfWork unitOfWork, final CaseId caseId,
                                             final PlayerId authorizedBy, final Instant releasedAt) {
        if (caseId == null || authorizedBy == null || releasedAt == null) {
            throw new NullPointerException("release argument");
        }
        final JdbcUnitOfWork jdbc = JdbcSupport.require(unitOfWork, true);
        return JdbcSupport.execute(jdbc, new JdbcSupport.SqlOperation<Boolean>() {
            @Override public Result<Boolean> execute() throws SQLException {
                try (PreparedStatement statement = jdbc.connection().prepareStatement(
                        "UPDATE zbw_legal_hold SET released_at = ?, released_by = ? "
                                + "WHERE case_id = ? AND released_at IS NULL")) {
                    statement.setQueryTimeout(JdbcSupport.timeoutSeconds(jdbc));
                    statement.setLong(1, releasedAt.toEpochMilli());
                    statement.setString(2, authorizedBy.toString());
                    statement.setString(3, caseId.toString());
                    return Result.success(statement.executeUpdate() == 1);
                }
            }
        });
    }

    @Override public Result<Boolean> tombstone(final UnitOfWork unitOfWork, final RecordKey key,
                                               final Instant deletedAt) {
        if (key == null || deletedAt == null) { throw new NullPointerException("tombstone argument"); }
        final JdbcUnitOfWork jdbc = JdbcSupport.require(unitOfWork, true);
        return JdbcSupport.execute(jdbc, new JdbcSupport.SqlOperation<Boolean>() {
            @Override public Result<Boolean> execute() throws SQLException {
                try (PreparedStatement statement = jdbc.connection().prepareStatement(
                        "INSERT INTO zbw_tombstone(aggregate_type, aggregate_id, deleted_at) VALUES(?, ?, ?)")) {
                    statement.setQueryTimeout(JdbcSupport.timeoutSeconds(jdbc));
                    bindKey(statement, key);
                    statement.setLong(3, deletedAt.toEpochMilli());
                    try { return Result.success(statement.executeUpdate() == 1); }
                    catch (SQLException exception) {
                        if (SqlErrors.duplicate(exception)) { return Result.success(false); }
                        throw exception;
                    }
                }
            }
        });
    }

    private static void bindKey(final PreparedStatement statement, final RecordKey key) throws SQLException {
        statement.setString(1, key.aggregateType().toString());
        statement.setString(2, key.aggregateId().toString());
    }
}
