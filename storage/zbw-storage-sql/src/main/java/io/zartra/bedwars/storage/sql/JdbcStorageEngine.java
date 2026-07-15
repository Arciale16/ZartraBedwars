package io.zartra.bedwars.storage.sql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.storage.api.MessageRepository;
import io.zartra.bedwars.storage.api.RetentionRepository;
import io.zartra.bedwars.storage.api.StorageEngine;
import io.zartra.bedwars.storage.api.StorageRepository;
import io.zartra.bedwars.storage.api.TransactionOptions;
import io.zartra.bedwars.storage.api.UnitOfWork;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/** Hikari-backed SQL engine for SQLite, MySQL and MariaDB. */
public final class JdbcStorageEngine implements StorageEngine {
    private final EngineKind kind;
    private final HikariDataSource dataSource;
    private final StorageRepository records;
    private final MessageRepository messages;
    private final RetentionRepository retention;
    private final AtomicBoolean closed;

    private JdbcStorageEngine(final EngineKind kind, final HikariDataSource dataSource) {
        this.kind = kind;
        this.dataSource = dataSource;
        this.records = new JdbcStorageRepository();
        this.messages = new JdbcMessageRepository();
        this.retention = new JdbcRetentionRepository();
        this.closed = new AtomicBoolean(false);
    }

    /** Opens a bounded pool, applies validated migrations and returns a ready engine. */
    public static Result<JdbcStorageEngine> open(final SqlStorageConfiguration configuration) {
        if (configuration == null) { throw new NullPointerException("configuration"); }
        final HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl(configuration.jdbcUrl());
        hikari.setUsername(configuration.username());
        final char[] secret = configuration.password();
        try {
            hikari.setPassword(new String(secret));
        } finally {
            Arrays.fill(secret, '\0');
        }
        hikari.setPoolName("zbw-" + configuration.engineKind().name().toLowerCase());
        hikari.setMaximumPoolSize(configuration.maximumPoolSize());
        hikari.setMinimumIdle(configuration.engineKind() == EngineKind.SQLITE
                ? 1 : Math.min(2, configuration.maximumPoolSize()));
        hikari.setConnectionTimeout(configuration.connectionTimeout().toMillis());
        hikari.setValidationTimeout(Math.min(5000L, configuration.connectionTimeout().toMillis()));
        hikari.setAutoCommit(false);
        if (configuration.engineKind() == EngineKind.SQLITE) {
            hikari.setConnectionInitSql("PRAGMA foreign_keys = ON");
        }
        final HikariDataSource source;
        try {
            source = new HikariDataSource(hikari);
        } catch (RuntimeException exception) {
            return Result.failure(SqlErrors.CLOSED);
        }
        final int querySeconds = (int) Math.max(1L,
                (configuration.queryTimeout().toMillis() + 999L) / 1000L);
        try (Connection connection = source.getConnection()) {
            connection.setAutoCommit(false);
            final Result<SchemaMigrator.MigrationReport> migration =
                    new SchemaMigrator(configuration.engineKind(), querySeconds).migrate(connection);
            if (migration.isFailure()) {
                connection.rollback();
                source.close();
                return Result.failure(migration.error().get());
            }
            connection.commit();
        } catch (SQLException exception) {
            source.close();
            return Result.failure(SqlErrors.classify(exception));
        }
        return Result.success(new JdbcStorageEngine(configuration.engineKind(), source));
    }

    @Override public EngineKind kind() { return kind; }

    @Override public Result<UnitOfWork> begin(final TransactionOptions options) {
        if (options == null) { throw new NullPointerException("options"); }
        if (closed.get()) { return Result.failure(SqlErrors.CLOSED); }
        try {
            return Result.<UnitOfWork>success(new JdbcUnitOfWork(dataSource.getConnection(), options, kind));
        } catch (SQLException exception) {
            return Result.failure(SqlErrors.classify(exception));
        }
    }

    @Override public StorageRepository records() { return records; }
    @Override public MessageRepository messages() { return messages; }
    @Override public RetentionRepository retention() { return retention; }

    /** @return sanitized instantaneous pool counters */
    public PoolHealth poolHealth() {
        final HikariPoolMXBean metrics = dataSource.getHikariPoolMXBean();
        if (metrics == null) { return PoolHealth.of(0, 0, 0, 0); }
        return PoolHealth.of(metrics.getActiveConnections(), metrics.getIdleConnections(),
                metrics.getTotalConnections(), metrics.getThreadsAwaitingConnection());
    }

    @Override public void close() {
        if (closed.compareAndSet(false, true)) { dataSource.close(); }
    }
}
