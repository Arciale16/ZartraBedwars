package io.zartra.bedwars.party.sql;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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

/** Checksum-locked native party schema migration. */
public final class PartySchemaMigrator {
    private static final int VERSION = 2101;
    private static final List<String> SQL = Arrays.asList(
            "CREATE TABLE IF NOT EXISTS zbw_schema_history (version INTEGER PRIMARY KEY, description VARCHAR(160) NOT NULL, checksum VARCHAR(64) NOT NULL, installed_at BIGINT NOT NULL)",
            "CREATE TABLE parties (party_id VARCHAR(36) PRIMARY KEY, state VARCHAR(24) NOT NULL, leader_id VARCHAR(36) NOT NULL, privacy VARCHAR(24) NOT NULL, migration_target VARCHAR(160), revision BIGINT NOT NULL)",
            "CREATE TABLE party_members (party_id VARCHAR(36) NOT NULL, member_id VARCHAR(36) NOT NULL UNIQUE, member_order INTEGER NOT NULL, PRIMARY KEY(party_id,member_id))",
            "CREATE TABLE party_invitations (party_id VARCHAR(36) NOT NULL, invitee_id VARCHAR(36) NOT NULL UNIQUE, invited_by VARCHAR(36) NOT NULL, created_at BIGINT NOT NULL, expires_at BIGINT NOT NULL, PRIMARY KEY(party_id,invitee_id))");

    /**
     * Applies the party schema off-thread.
     *
     * @param source JDBC source
     * @param executor bounded storage executor
     * @return true only when the migration was newly installed
     */
    public CompletionStage<Boolean> migrate(final DataSource source, final Executor executor) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(executor, "executor");
        return CompletableFuture.supplyAsync(() -> migrateNow(source), executor);
    }

    /** @return canonical SHA-256 migration checksum */
    public String checksum() { return sha256(String.join("\n", SQL)); }

    private boolean migrateNow(final DataSource source) {
        try (Connection connection = source.getConnection()) {
            connection.setAutoCommit(false);
            try {
                execute(connection, SQL.get(0));
                String installed = installed(connection);
                if (installed != null) {
                    if (!checksum().equals(installed)) {
                        throw new PartyPersistenceException("party schema checksum drift");
                    }
                    connection.commit();
                    return false;
                }
                for (int index = 1; index < SQL.size(); index++) {
                    execute(connection, SQL.get(index));
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO zbw_schema_history VALUES(?,?,?,?)")) {
                    statement.setInt(1, VERSION);
                    statement.setString(2, "M21 native party persistence");
                    statement.setString(3, checksum());
                    statement.setLong(4, Instant.now().toEpochMilli());
                    statement.executeUpdate();
                }
                connection.commit();
                return true;
            } catch (RuntimeException | SQLException failure) {
                rollback(connection);
                if (failure instanceof PartyPersistenceException) {
                    throw (PartyPersistenceException) failure;
                }
                throw new PartyPersistenceException("party migration failed", failure);
            }
        } catch (SQLException failure) {
            throw new PartyPersistenceException("party connection failed", failure);
        }
    }

    private String installed(final Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT checksum FROM zbw_schema_history WHERE version=?")) {
            statement.setInt(1, VERSION);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? rows.getString(1) : null;
            }
        }
    }

    private static void execute(final Connection connection, final String sql)
            throws SQLException {
        try (Statement statement = connection.createStatement()) { statement.execute(sql); }
    }

    private static void rollback(final Connection connection) {
        try { connection.rollback(); } catch (SQLException ignored) { }
    }

    private static String sha256(final String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder output = new StringBuilder(64);
            for (byte item : digest) { output.append(String.format("%02x", item & 255)); }
            return output.toString();
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }
}
