package io.zartra.bedwars.atlas.sql;

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

/** Checksum-locked M18 schema migration with an isolated identity vault. */
public final class AtlasSchemaMigrator {
    private static final int VERSION = 1801;
    private static final List<String> SQL = Arrays.asList(
            "CREATE TABLE IF NOT EXISTS zbw_schema_history (version INTEGER PRIMARY KEY, description VARCHAR(160) NOT NULL, checksum VARCHAR(64) NOT NULL, installed_at BIGINT NOT NULL)",
            "CREATE TABLE atlas_cases (case_id VARCHAR(36) PRIMARY KEY, status VARCHAR(32) NOT NULL, source VARCHAR(32) NOT NULL, created_at BIGINT NOT NULL, category VARCHAR(80) NOT NULL, source_ref VARCHAR(160) NOT NULL UNIQUE, priority INTEGER NOT NULL, schema_version INTEGER NOT NULL, revision BIGINT NOT NULL)",
            "CREATE TABLE atlas_evidence (case_id VARCHAR(36) NOT NULL, evidence_id VARCHAR(36) NOT NULL, type VARCHAR(32) NOT NULL, replay_id VARCHAR(36), external_ref VARCHAR(160), start_ms BIGINT NOT NULL, end_ms BIGINT NOT NULL, PRIMARY KEY(case_id,evidence_id))",
            "CREATE TABLE atlas_reviews (review_id VARCHAR(36) PRIMARY KEY, case_id VARCHAR(36) NOT NULL, reviewer_id VARCHAR(36) NOT NULL, decision VARCHAR(32) NOT NULL, verdict VARCHAR(40) NOT NULL, reason VARCHAR(40) NOT NULL, submitted_at BIGINT NOT NULL, interaction_ms BIGINT NOT NULL, schema_version INTEGER NOT NULL, UNIQUE(case_id,reviewer_id))",
            "CREATE TABLE atlas_reservations (case_id VARCHAR(36) PRIMARY KEY, reviewer_id VARCHAR(36) NOT NULL, expires_at BIGINT NOT NULL)",
            "CREATE TABLE atlas_reviewer_profiles (reviewer_id VARCHAR(36) PRIMARY KEY, lifetime_reviews BIGINT NOT NULL, recent_reviews BIGINT NOT NULL, accuracy DOUBLE NOT NULL, confidence DOUBLE NOT NULL, reputation DOUBLE NOT NULL, updated_at BIGINT NOT NULL)",
            "CREATE TABLE atlas_audit (sequence_no BIGINT PRIMARY KEY, actor_ref VARCHAR(160) NOT NULL, action_ref VARCHAR(160) NOT NULL, target_ref VARCHAR(160) NOT NULL, occurred_at BIGINT NOT NULL, result_ref VARCHAR(160) NOT NULL, before_ref VARCHAR(160) NOT NULL, after_ref VARCHAR(160) NOT NULL)",
            "CREATE TABLE atlas_identity_vault (case_id VARCHAR(36) PRIMARY KEY, encrypted_identity BLOB NOT NULL)");

    /** Applies the schema asynchronously and rejects checksum drift. */
    public CompletionStage<Boolean> migrate(final DataSource source, final Executor executor) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(executor, "executor");
        return CompletableFuture.supplyAsync(() -> migrateNow(source), executor);
    }

    /** Returns the canonical schema checksum. */
    public String checksum() { return sha256(String.join("\n", SQL)); }

    private boolean migrateNow(final DataSource source) {
        try (Connection connection = source.getConnection()) {
            connection.setAutoCommit(false);
            try {
                execute(connection, SQL.get(0));
                String installed = installed(connection);
                if (installed != null) {
                    if (!checksum().equals(installed)) {
                        throw new AtlasPersistenceException("Atlas schema checksum drift");
                    }
                    connection.commit();
                    return false;
                }
                for (int index = 1; index < SQL.size(); index++) { execute(connection, SQL.get(index)); }
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO zbw_schema_history VALUES(?,?,?,?)")) {
                    statement.setInt(1, VERSION);
                    statement.setString(2, "M18 Atlas persistence");
                    statement.setString(3, checksum());
                    statement.setLong(4, Instant.now().toEpochMilli());
                    statement.executeUpdate();
                }
                connection.commit();
                return true;
            } catch (RuntimeException | SQLException failure) {
                rollback(connection);
                if (failure instanceof AtlasPersistenceException) {
                    throw (AtlasPersistenceException) failure;
                }
                throw new AtlasPersistenceException("Atlas migration failed", failure);
            }
        } catch (SQLException failure) {
            throw new AtlasPersistenceException("Atlas connection failed", failure);
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

    private static void execute(final Connection connection, final String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) { statement.execute(sql); }
    }

    private static void rollback(final Connection connection) {
        try { connection.rollback(); } catch (SQLException ignored) { }
    }

    private static String sha256(final String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                    value.getBytes(StandardCharsets.UTF_8));
            StringBuilder output = new StringBuilder(64);
            for (byte item : digest) { output.append(String.format("%02x", item & 255)); }
            return output.toString();
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }
}
