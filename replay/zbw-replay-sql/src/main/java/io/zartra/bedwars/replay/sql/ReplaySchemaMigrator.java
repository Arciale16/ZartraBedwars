package io.zartra.bedwars.replay.sql;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import javax.sql.DataSource;

/** Checksum-locked M17 replay metadata and ordered-event schema migration. */
public final class ReplaySchemaMigrator {
    private static final int VERSION = 1701;
    private static final List<String> SQL = Arrays.asList(
            "CREATE TABLE IF NOT EXISTS zbw_schema_history (version INTEGER PRIMARY KEY, description VARCHAR(160) NOT NULL, checksum VARCHAR(64) NOT NULL, installed_at BIGINT NOT NULL)",
            "CREATE TABLE IF NOT EXISTS replay_metadata (replay_id VARCHAR(36) PRIMARY KEY, match_id VARCHAR(36) NOT NULL, created_at BIGINT NOT NULL, format_version INTEGER NOT NULL, protected_evidence SMALLINT NOT NULL)",
            "CREATE TABLE IF NOT EXISTS replay_participants (replay_id VARCHAR(36) NOT NULL, player_id VARCHAR(36) NOT NULL, PRIMARY KEY(replay_id, player_id))",
            "CREATE TABLE IF NOT EXISTS replay_sessions (replay_id VARCHAR(36) PRIMARY KEY, state VARCHAR(24) NOT NULL, failure_reason VARCHAR(256))",
            "CREATE TABLE IF NOT EXISTS replay_events (replay_id VARCHAR(36) NOT NULL, sequence_number BIGINT NOT NULL, event_id VARCHAR(160) NOT NULL, offset_millis BIGINT NOT NULL, occurred_at BIGINT NOT NULL, source VARCHAR(24) NOT NULL, event_type VARCHAR(160) NOT NULL, attributes TEXT NOT NULL, PRIMARY KEY(replay_id, sequence_number), UNIQUE(replay_id, event_id))");

    /** Applies the migration on the supplied worker executor and rejects checksum drift. */
    public CompletionStage<Report> migrate(final DataSource dataSource, final Executor executor) {
        Objects.requireNonNull(dataSource, "dataSource");
        Objects.requireNonNull(executor, "executor");
        return CompletableFuture.supplyAsync(() -> migrateNow(dataSource), executor);
    }

    /** Returns the canonical migration checksum. */ public String checksum() {
        return sha256(String.join("\n", SQL));
    }

    private Report migrateNow(final DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                execute(connection, SQL.get(0));
                final String installed = installedChecksum(connection);
                if (installed != null) {
                    if (!installed.equals(checksum())) {
                        throw new ReplayPersistenceException("replay schema checksum drift");
                    }
                    connection.commit();
                    return new Report(false, checksum());
                }
                for (int index = 1; index < SQL.size(); index++) { execute(connection, SQL.get(index)); }
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO zbw_schema_history(version,description,checksum,installed_at) VALUES(?,?,?,?)")) {
                    statement.setInt(1, VERSION);
                    statement.setString(2, "M17 replay persistence");
                    statement.setString(3, checksum());
                    statement.setLong(4, Instant.now().toEpochMilli());
                    statement.executeUpdate();
                }
                connection.commit();
                return new Report(true, checksum());
            } catch (RuntimeException | SQLException failure) {
                rollback(connection);
                if (failure instanceof ReplayPersistenceException) {
                    throw (ReplayPersistenceException) failure;
                }
                throw new ReplayPersistenceException("replay schema migration failed", failure);
            }
        } catch (SQLException failure) {
            throw new ReplayPersistenceException("replay schema connection failed", failure);
        }
    }

    private static String installedChecksum(final Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT checksum FROM zbw_schema_history WHERE version=?")) {
            statement.setInt(1, VERSION);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getString(1) : null;
            }
        }
    }

    private static void execute(final Connection connection, final String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) { statement.execute(sql); }
    }

    private static void rollback(final Connection connection) {
        try { connection.rollback(); } catch (SQLException ignored) { }
    }

    private static String sha256(final String value) {
        try {
            final byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                    value.getBytes(StandardCharsets.UTF_8));
            final StringBuilder result = new StringBuilder(64);
            for (byte item : digest) { result.append(String.format("%02x", item & 0xff)); }
            return result.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    /** Immutable migration result. */
    public static final class Report {
        private final boolean applied;
        private final String checksum;
        private Report(final boolean applied, final String checksum) {
            this.applied = applied;
            this.checksum = checksum;
        }
        /** Returns whether this call installed the schema. */ public boolean applied() { return applied; }
        /** Returns the canonical installed checksum. */ public String checksum() { return checksum; }
    }
}
