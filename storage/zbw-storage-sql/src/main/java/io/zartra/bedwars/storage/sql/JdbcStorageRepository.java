package io.zartra.bedwars.storage.sql;

import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.storage.api.RecordKey;
import io.zartra.bedwars.storage.api.RecordRevision;
import io.zartra.bedwars.storage.api.StorageRepository;
import io.zartra.bedwars.storage.api.StoredRecord;
import io.zartra.bedwars.storage.api.UnitOfWork;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;

/** Prepared-statement aggregate repository with optimistic concurrency. */
final class JdbcStorageRepository implements StorageRepository {
    @Override public Result<Optional<StoredRecord>> find(final UnitOfWork unitOfWork,
                                                         final RecordKey key) {
        if (key == null) { throw new NullPointerException("key"); }
        final JdbcUnitOfWork jdbc = JdbcSupport.require(unitOfWork, false);
        return JdbcSupport.execute(jdbc, new JdbcSupport.SqlOperation<Optional<StoredRecord>>() {
            @Override public Result<Optional<StoredRecord>> execute() throws SQLException {
                try (PreparedStatement statement = jdbc.connection().prepareStatement(
                        "SELECT revision, schema_version, payload, updated_at FROM zbw_records "
                                + "WHERE aggregate_type = ? AND aggregate_id = ?")) {
                    statement.setQueryTimeout(JdbcSupport.timeoutSeconds(jdbc));
                    bindKey(statement, key);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) { return Result.success(Optional.<StoredRecord>empty()); }
                        return Result.success(Optional.of(StoredRecord.of(key,
                                RecordRevision.of(result.getLong(1)), result.getInt(2),
                                result.getBytes(3), Instant.ofEpochMilli(result.getLong(4)))));
                    }
                }
            }
        });
    }

    @Override public Result<StoredRecord> save(final UnitOfWork unitOfWork,
                                               final StoredRecord record,
                                               final RecordRevision expectedRevision) {
        if (record == null) { throw new NullPointerException("record"); }
        if (expectedRevision == null) { throw new NullPointerException("expectedRevision"); }
        final JdbcUnitOfWork jdbc = JdbcSupport.require(unitOfWork, true);
        return JdbcSupport.execute(jdbc, new JdbcSupport.SqlOperation<StoredRecord>() {
            @Override public Result<StoredRecord> execute() throws SQLException {
                final RecordRevision next = expectedRevision.next();
                final int changed;
                if (expectedRevision.equals(RecordRevision.initial())) {
                    try (PreparedStatement statement = jdbc.connection().prepareStatement(
                            "INSERT INTO zbw_records(aggregate_type, aggregate_id, revision, "
                                    + "schema_version, payload, updated_at) VALUES(?, ?, ?, ?, ?, ?)")) {
                        statement.setQueryTimeout(JdbcSupport.timeoutSeconds(jdbc));
                        bindRecord(statement, record, next);
                        try {
                            changed = statement.executeUpdate();
                        } catch (SQLException exception) {
                            if (SqlErrors.duplicate(exception)) { return Result.failure(SqlErrors.CONFLICT); }
                            throw exception;
                        }
                    }
                } else {
                    try (PreparedStatement statement = jdbc.connection().prepareStatement(
                            "UPDATE zbw_records SET revision = ?, schema_version = ?, payload = ?, "
                                    + "updated_at = ? WHERE aggregate_type = ? AND aggregate_id = ? AND revision = ?")) {
                        statement.setQueryTimeout(JdbcSupport.timeoutSeconds(jdbc));
                        statement.setLong(1, next.value());
                        statement.setInt(2, record.schemaVersion());
                        statement.setBytes(3, record.payload());
                        statement.setLong(4, record.updatedAt().toEpochMilli());
                        statement.setString(5, record.key().aggregateType().toString());
                        statement.setString(6, record.key().aggregateId().toString());
                        statement.setLong(7, expectedRevision.value());
                        changed = statement.executeUpdate();
                    }
                }
                if (changed != 1) { return Result.failure(SqlErrors.CONFLICT); }
                return Result.success(StoredRecord.of(record.key(), next, record.schemaVersion(),
                        record.payload(), record.updatedAt()));
            }
        });
    }

    @Override public Result<Boolean> delete(final UnitOfWork unitOfWork, final RecordKey key,
                                            final RecordRevision expectedRevision) {
        if (key == null) { throw new NullPointerException("key"); }
        if (expectedRevision == null) { throw new NullPointerException("expectedRevision"); }
        final JdbcUnitOfWork jdbc = JdbcSupport.require(unitOfWork, true);
        return JdbcSupport.execute(jdbc, new JdbcSupport.SqlOperation<Boolean>() {
            @Override public Result<Boolean> execute() throws SQLException {
                try (PreparedStatement statement = jdbc.connection().prepareStatement(
                        "DELETE FROM zbw_records WHERE aggregate_type = ? AND aggregate_id = ? AND revision = ?")) {
                    statement.setQueryTimeout(JdbcSupport.timeoutSeconds(jdbc));
                    bindKey(statement, key);
                    statement.setLong(3, expectedRevision.value());
                    return Result.success(statement.executeUpdate() == 1);
                }
            }
        });
    }

    private static void bindKey(final PreparedStatement statement, final RecordKey key) throws SQLException {
        statement.setString(1, key.aggregateType().toString());
        statement.setString(2, key.aggregateId().toString());
    }
    private static void bindRecord(final PreparedStatement statement, final StoredRecord record,
                                   final RecordRevision revision) throws SQLException {
        statement.setString(1, record.key().aggregateType().toString());
        statement.setString(2, record.key().aggregateId().toString());
        statement.setLong(3, revision.value());
        statement.setInt(4, record.schemaVersion());
        statement.setBytes(5, record.payload());
        statement.setLong(6, record.updatedAt().toEpochMilli());
    }
}
