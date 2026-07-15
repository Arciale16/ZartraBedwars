package io.zartra.bedwars.storage.sql;

import io.zartra.bedwars.api.event.EventMetadata;
import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.EventId;
import io.zartra.bedwars.api.identity.EventTypeId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.storage.api.MessageEnvelope;
import io.zartra.bedwars.storage.api.MessageRepository;
import io.zartra.bedwars.storage.api.UnitOfWork;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Durable transactional outbox/inbox repository with uniqueness-based idempotency. */
final class JdbcMessageRepository implements MessageRepository {
    @Override public Result<Boolean> enqueue(final UnitOfWork unitOfWork,
                                             final MessageEnvelope envelope) {
        if (envelope == null) { throw new NullPointerException("envelope"); }
        final JdbcUnitOfWork jdbc = JdbcSupport.require(unitOfWork, true);
        return JdbcSupport.execute(jdbc, new JdbcSupport.SqlOperation<Boolean>() {
            @Override public Result<Boolean> execute() throws SQLException {
                try (PreparedStatement statement = jdbc.connection().prepareStatement(
                        "INSERT INTO zbw_outbox(operation_id, event_id, event_type, correlation_id, "
                                + "occurred_at, sequence_no, schema_version, thread_context, payload, "
                                + "available_at, claimed_until, delivered_at) "
                                + "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, NULL)")) {
                    statement.setQueryTimeout(JdbcSupport.timeoutSeconds(jdbc));
                    bindEnvelope(statement, envelope);
                    try {
                        return Result.success(statement.executeUpdate() == 1);
                    } catch (SQLException exception) {
                        if (SqlErrors.duplicate(exception)) { return Result.success(false); }
                        throw exception;
                    }
                }
            }
        });
    }

    @Override public Result<List<MessageEnvelope>> claim(final UnitOfWork unitOfWork,
                                                         final Instant now, final int maximum,
                                                         final Duration lease) {
        if (now == null) { throw new NullPointerException("now"); }
        if (maximum < 1 || maximum > 1000) { throw new IllegalArgumentException("maximum must be 1..1000"); }
        if (lease == null || lease.isNegative() || lease.isZero()) {
            throw new IllegalArgumentException("lease must be positive");
        }
        final JdbcUnitOfWork jdbc = JdbcSupport.require(unitOfWork, true);
        return JdbcSupport.execute(jdbc, new JdbcSupport.SqlOperation<List<MessageEnvelope>>() {
            @Override public Result<List<MessageEnvelope>> execute() throws SQLException {
                final List<MessageEnvelope> candidates = new ArrayList<MessageEnvelope>();
                try (PreparedStatement statement = jdbc.connection().prepareStatement(
                        "SELECT operation_id, event_id, event_type, correlation_id, occurred_at, "
                                + "sequence_no, schema_version, thread_context, payload, available_at "
                                + "FROM zbw_outbox WHERE delivered_at IS NULL AND available_at <= ? "
                                + "AND (claimed_until IS NULL OR claimed_until < ?) "
                                + "ORDER BY sequence_no, available_at, operation_id")) {
                    statement.setQueryTimeout(JdbcSupport.timeoutSeconds(jdbc));
                    statement.setLong(1, now.toEpochMilli());
                    statement.setLong(2, now.toEpochMilli());
                    statement.setMaxRows(maximum);
                    try (ResultSet result = statement.executeQuery()) {
                        while (result.next()) { candidates.add(readEnvelope(result)); }
                    }
                }
                final List<MessageEnvelope> claimed = new ArrayList<MessageEnvelope>();
                final long claimedUntil = Math.addExact(now.toEpochMilli(), lease.toMillis());
                for (MessageEnvelope candidate : candidates) {
                    try (PreparedStatement update = jdbc.connection().prepareStatement(
                            "UPDATE zbw_outbox SET claimed_until = ? WHERE operation_id = ? "
                                    + "AND delivered_at IS NULL AND (claimed_until IS NULL OR claimed_until < ?)")) {
                        update.setQueryTimeout(JdbcSupport.timeoutSeconds(jdbc));
                        update.setLong(1, claimedUntil);
                        update.setString(2, candidate.operationId().toString());
                        update.setLong(3, now.toEpochMilli());
                        if (update.executeUpdate() == 1) { claimed.add(candidate); }
                    }
                }
                if (claimed.isEmpty()) { return Result.success(java.util.Collections.<MessageEnvelope>emptyList()); }
                return Result.success(MessageRepository.Batches.bounded(claimed, maximum));
            }
        });
    }

    @Override public Result<Boolean> acknowledge(final UnitOfWork unitOfWork,
                                                 final IdempotencyKey operationId) {
        if (operationId == null) { throw new NullPointerException("operationId"); }
        final JdbcUnitOfWork jdbc = JdbcSupport.require(unitOfWork, true);
        return JdbcSupport.execute(jdbc, new JdbcSupport.SqlOperation<Boolean>() {
            @Override public Result<Boolean> execute() throws SQLException {
                try (PreparedStatement statement = jdbc.connection().prepareStatement(
                        "UPDATE zbw_outbox SET delivered_at = ?, claimed_until = NULL "
                                + "WHERE operation_id = ? AND delivered_at IS NULL")) {
                    statement.setQueryTimeout(JdbcSupport.timeoutSeconds(jdbc));
                    statement.setLong(1, Instant.now().toEpochMilli());
                    statement.setString(2, operationId.toString());
                    return Result.success(statement.executeUpdate() == 1);
                }
            }
        });
    }

    @Override public Result<Boolean> receive(final UnitOfWork unitOfWork,
                                             final MessageEnvelope envelope) {
        if (envelope == null) { throw new NullPointerException("envelope"); }
        final JdbcUnitOfWork jdbc = JdbcSupport.require(unitOfWork, true);
        return JdbcSupport.execute(jdbc, new JdbcSupport.SqlOperation<Boolean>() {
            @Override public Result<Boolean> execute() throws SQLException {
                try (PreparedStatement statement = jdbc.connection().prepareStatement(
                        "INSERT INTO zbw_inbox(operation_id, event_id, received_at) VALUES(?, ?, ?)")) {
                    statement.setQueryTimeout(JdbcSupport.timeoutSeconds(jdbc));
                    statement.setString(1, envelope.operationId().toString());
                    statement.setString(2, envelope.metadata().eventId().toString());
                    statement.setLong(3, Instant.now().toEpochMilli());
                    try {
                        return Result.success(statement.executeUpdate() == 1);
                    } catch (SQLException exception) {
                        if (SqlErrors.duplicate(exception)) { return Result.success(false); }
                        throw exception;
                    }
                }
            }
        });
    }

    private static void bindEnvelope(final PreparedStatement statement,
                                     final MessageEnvelope envelope) throws SQLException {
        final EventMetadata metadata = envelope.metadata();
        statement.setString(1, envelope.operationId().toString());
        statement.setString(2, metadata.eventId().toString());
        statement.setString(3, metadata.eventType().toString());
        statement.setString(4, metadata.correlationId().toString());
        statement.setLong(5, metadata.occurredAt().toEpochMilli());
        statement.setLong(6, metadata.sequence());
        statement.setInt(7, metadata.schemaVersion());
        statement.setString(8, metadata.threadContext().name());
        statement.setBytes(9, envelope.payload());
        statement.setLong(10, envelope.availableAt().toEpochMilli());
    }

    private static MessageEnvelope readEnvelope(final ResultSet result) throws SQLException {
        final EventMetadata metadata = EventMetadata.of(EventId.parse(result.getString(2)),
                EventTypeId.parse(result.getString(3)), CorrelationId.parse(result.getString(4)),
                Instant.ofEpochMilli(result.getLong(5)), result.getLong(6), result.getInt(7),
                EventMetadata.ThreadContext.valueOf(result.getString(8)));
        return MessageEnvelope.of(IdempotencyKey.parse(result.getString(1)), metadata,
                result.getBytes(9), Instant.ofEpochMilli(result.getLong(10)));
    }
}
