package io.zartra.bedwars.storage.sql;

import io.zartra.bedwars.api.result.Result;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;

/** Checksum-locked M13 state migration layered on the M04/M12 schema history. */
public final class M13SchemaMigrator {
    private static final int VERSION = 13;
    private static final String[] SQL = {
        table("objective_progress"), table("quest_state"), table("achievement_state"),
        table("challenge_state"), table("season_progress"),
        "CREATE TABLE IF NOT EXISTS m13_event_claims (idempotency_key VARCHAR(129) PRIMARY KEY, occurred_at BIGINT NOT NULL)"
    };
    private final int timeoutSeconds;

    /** Creates a migration runner with bounded statements. */
    public M13SchemaMigrator(final int timeoutSeconds) {
        if (timeoutSeconds < 1) { throw new IllegalArgumentException("timeoutSeconds must be positive"); }
        this.timeoutSeconds = timeoutSeconds;
    }
    /** @return canonical migration checksum */ public String checksum() { return sha256(String.join("\n", SQL)); }
    /** Applies the migration once and rejects installed checksum drift. */
    public Result<Report> migrate(final Connection connection) {
        if (connection == null) { throw new NullPointerException("connection"); }
        try {
            final String installed = installed(connection);
            if (installed != null) {
                return installed.equals(checksum()) ? Result.success(new Report(false, checksum()))
                        : Result.failure(SqlErrors.CONFLICT);
            }
            for (String sql : SQL) {
                try (Statement statement = connection.createStatement()) {
                    statement.setQueryTimeout(timeoutSeconds);
                    statement.execute(sql);
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO zbw_schema_history(version,description,checksum,installed_at) VALUES(?,?,?,?)")) {
                statement.setQueryTimeout(timeoutSeconds);
                statement.setInt(1, VERSION);
                statement.setString(2, "m13_objective_runtime");
                statement.setString(3, checksum());
                statement.setLong(4, Instant.now().toEpochMilli());
                statement.executeUpdate();
            }
            return Result.success(new Report(true, checksum()));
        } catch (SQLException failure) { return Result.failure(SqlErrors.classify(failure)); }
    }
    private String installed(final Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT checksum FROM zbw_schema_history WHERE version=?")) {
            statement.setQueryTimeout(timeoutSeconds);
            statement.setInt(1, VERSION);
            try (ResultSet result = statement.executeQuery()) { return result.next() ? result.getString(1) : null; }
        }
    }
    private static String table(final String name) {
        return "CREATE TABLE IF NOT EXISTS " + name
                + " (player_id VARCHAR(36) NOT NULL, definition_id VARCHAR(129) NOT NULL,"
                + " payload BLOB NOT NULL, revision BIGINT NOT NULL, updated_at BIGINT NOT NULL,"
                + " PRIMARY KEY(player_id,definition_id))";
    }
    private static String sha256(final String input) {
        try {
            final byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            final StringBuilder value = new StringBuilder(64);
            for (byte part : digest) { value.append(String.format("%02x", part & 0xff)); }
            return value.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }
    /** Immutable migration evidence. */
    public static final class Report {
        private final boolean applied;
        private final String checksum;
        private Report(final boolean applied, final String checksum) {
            this.applied = applied;
            this.checksum = checksum;
        }
        /** @return whether this invocation installed M13 */ public boolean applied() { return applied; }
        /** @return installed checksum */ public String checksum() { return checksum; }
    }
}
