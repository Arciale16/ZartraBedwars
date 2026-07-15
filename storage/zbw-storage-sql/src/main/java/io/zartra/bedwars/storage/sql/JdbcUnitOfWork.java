package io.zartra.bedwars.storage.sql;

import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.storage.api.TransactionOptions;
import io.zartra.bedwars.storage.api.UnitOfWork;
import io.zartra.bedwars.storage.api.StorageEngine.EngineKind;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/** JDBC-backed transaction handle with strict creator-thread ownership. */
final class JdbcUnitOfWork implements UnitOfWork {
    private final Connection connection;
    private final TransactionOptions options;
    private final Thread ownerThread;
    private State state;

    JdbcUnitOfWork(final Connection connection, final TransactionOptions options,
                   final EngineKind kind) throws SQLException {
        this.connection = Objects.requireNonNull(connection, "connection");
        this.options = Objects.requireNonNull(options, "options");
        this.ownerThread = Thread.currentThread();
        this.state = State.ACTIVE;
        connection.setAutoCommit(false);
        if (kind != EngineKind.SQLITE) {
            connection.setReadOnly(options.accessMode() == TransactionOptions.AccessMode.READ_ONLY);
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        } else {
            connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
        }
    }

    Connection connection() {
        requireOwner();
        if (state != State.ACTIVE) { throw new IllegalStateException("unit of work is not active"); }
        return connection;
    }

    TransactionOptions options() { return options; }

    @Override public State state() {
        requireOwner();
        return state;
    }

    @Override public Result<State> commit() {
        requireOwner();
        if (state != State.ACTIVE) { return Result.failure(SqlErrors.INVALID_TRANSACTION); }
        try {
            connection.commit();
            state = State.COMMITTED;
            connection.close();
            return Result.success(state);
        } catch (SQLException exception) {
            rollbackQuietly();
            return Result.failure(SqlErrors.classify(exception));
        }
    }

    @Override public Result<State> rollback() {
        requireOwner();
        if (state != State.ACTIVE) { return Result.failure(SqlErrors.INVALID_TRANSACTION); }
        try {
            connection.rollback();
            state = State.ROLLED_BACK;
            connection.close();
            return Result.success(state);
        } catch (SQLException exception) {
            state = State.ROLLED_BACK;
            closeQuietly();
            return Result.failure(SqlErrors.classify(exception));
        }
    }

    @Override public void close() {
        requireOwner();
        if (state == State.ACTIVE) { rollbackQuietly(); }
        if (state != State.CLOSED) {
            state = State.CLOSED;
            closeQuietly();
        }
    }

    private void requireOwner() {
        if (Thread.currentThread() != ownerThread) {
            throw new IllegalStateException("unit of work is thread-confined");
        }
    }
    private void rollbackQuietly() {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            state = State.ROLLED_BACK;
        }
        state = State.ROLLED_BACK;
        closeQuietly();
    }
    private void closeQuietly() {
        try {
            connection.close();
        } catch (SQLException ignored) {
            state = State.CLOSED;
        }
    }
}
