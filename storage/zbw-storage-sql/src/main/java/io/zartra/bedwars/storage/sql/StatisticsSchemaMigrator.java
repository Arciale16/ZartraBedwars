package io.zartra.bedwars.storage.sql;

import io.zartra.bedwars.api.result.Result;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;

/** Checksum-locked M15 statistics schema migration. */
public final class StatisticsSchemaMigrator {
    private static final int VERSION = 15;
    private static final String[] SQL = {
        "CREATE TABLE IF NOT EXISTS statistics_event_claims (idempotency_key VARCHAR(129) PRIMARY KEY, claimed_at BIGINT NOT NULL)",
        "CREATE TABLE IF NOT EXISTS statistics_match_aggregates (match_id VARCHAR(36) NOT NULL, statistic_id VARCHAR(129) NOT NULL, value BIGINT NOT NULL, revision BIGINT NOT NULL, actor VARCHAR(128) NOT NULL, correlation_id VARCHAR(36) NOT NULL, recorded_at BIGINT NOT NULL, PRIMARY KEY(match_id,statistic_id))",
        "CREATE TABLE IF NOT EXISTS statistics_player_aggregates (player_id VARCHAR(36) NOT NULL, statistic_id VARCHAR(129) NOT NULL, scope_id VARCHAR(129) NOT NULL, value BIGINT NOT NULL, revision BIGINT NOT NULL, actor VARCHAR(128) NOT NULL, correlation_id VARCHAR(36) NOT NULL, recorded_at BIGINT NOT NULL, PRIMARY KEY(player_id,statistic_id,scope_id))"
    };
    private final int timeoutSeconds;

    /** Creates a bounded migration runner. */
    public StatisticsSchemaMigrator(final int timeoutSeconds) {
        if (timeoutSeconds < 1) {
            throw new IllegalArgumentException("timeoutSeconds must be positive");
        }
        this.timeoutSeconds = timeoutSeconds;
    }

    /** @return stable schema checksum */
    public String checksum() {
        return sha256(String.join("\n", SQL));
    }

    /** Applies the migration once and rejects checksum drift. */
    public Result<Boolean> migrate(final Connection connection) {
        try {
            final String installed = installed(connection);
            if (installed != null) {
                return installed.equals(checksum())
                        ? Result.success(false) : Result.failure(SqlErrors.CONFLICT);
            }
            for (String sql : SQL) {
                try (Statement statement = connection.createStatement()) {
                    statement.setQueryTimeout(timeoutSeconds);
                    statement.execute(sql);
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO zbw_schema_history(version,description,checksum,installed_at) "
                            + "VALUES(?,?,?,?)")) {
                statement.setQueryTimeout(timeoutSeconds);
                statement.setInt(1, VERSION);
                statement.setString(2, "m15_statistics");
                statement.setString(3, checksum());
                statement.setLong(4, Instant.now().toEpochMilli());
                statement.executeUpdate();
            }
            return Result.success(true);
        } catch (SQLException failure) {
            return Result.failure(SqlErrors.classify(failure));
        }
    }

    private String installed(final Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT checksum FROM zbw_schema_history WHERE version=?")) {
            statement.setQueryTimeout(timeoutSeconds);
            statement.setInt(1, VERSION);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getString(1) : null;
            }
        }
    }

    private static String sha256(final String source) {
        try {
            final byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(source.getBytes(StandardCharsets.UTF_8));
            final StringBuilder result = new StringBuilder(64);
            for (byte value : digest) {
                result.append(String.format("%02x", value & 0xff));
            }
            return result.toString();
        } catch (Exception failure) {
            throw new IllegalStateException("SHA-256 unavailable", failure);
        }
    }
}
