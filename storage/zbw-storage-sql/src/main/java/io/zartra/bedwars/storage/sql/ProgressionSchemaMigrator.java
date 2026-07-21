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

/** Checksum-locked M12 migration that reuses the M04 schema history and infrastructure tables. */
public final class ProgressionSchemaMigrator {
    private static final int VERSION = 12;
    private static final String[] SQL = {
        "CREATE TABLE IF NOT EXISTS progression_accounts (player_id VARCHAR(36) PRIMARY KEY, payload BLOB NOT NULL, revision BIGINT NOT NULL)",
        "CREATE TABLE IF NOT EXISTS progression_xp_ledger (transaction_id VARCHAR(129) PRIMARY KEY, player_id VARCHAR(36) NOT NULL, idempotency_key VARCHAR(129) NOT NULL UNIQUE, payload BLOB NOT NULL, created_at BIGINT NOT NULL)",
        "CREATE TABLE IF NOT EXISTS progression_level_history (history_id VARCHAR(129) PRIMARY KEY, player_id VARCHAR(36) NOT NULL, payload BLOB NOT NULL, attained_at BIGINT NOT NULL)",
        "CREATE TABLE IF NOT EXISTS progression_prestige_history (history_id VARCHAR(129) PRIMARY KEY, player_id VARCHAR(36) NOT NULL, payload BLOB NOT NULL, attained_at BIGINT NOT NULL)",
        "CREATE TABLE IF NOT EXISTS currency_accounts (player_id VARCHAR(36) NOT NULL, currency_id VARCHAR(129) NOT NULL, payload BLOB NOT NULL, revision BIGINT NOT NULL, PRIMARY KEY(player_id,currency_id))",
        "CREATE TABLE IF NOT EXISTS economic_transactions (transaction_id VARCHAR(129) PRIMARY KEY, idempotency_key VARCHAR(129) NOT NULL UNIQUE, payload BLOB NOT NULL, created_at BIGINT NOT NULL)",
        "CREATE TABLE IF NOT EXISTS economic_transaction_entries (transaction_id VARCHAR(129) PRIMARY KEY, player_id VARCHAR(36) NOT NULL, currency_id VARCHAR(129) NOT NULL, delta BIGINT NOT NULL, resulting_balance BIGINT NOT NULL)",
        "CREATE TABLE IF NOT EXISTS reward_grants (idempotency_key VARCHAR(129) PRIMARY KEY, reward_id VARCHAR(129) NOT NULL, player_id VARCHAR(36) NOT NULL, payload BLOB NOT NULL)",
        "CREATE TABLE IF NOT EXISTS reward_deliveries (delivery_id VARCHAR(129) PRIMARY KEY, idempotency_key VARCHAR(129) NOT NULL UNIQUE, state VARCHAR(24) NOT NULL, attempts INTEGER NOT NULL, updated_at BIGINT NOT NULL)",
        "CREATE TABLE IF NOT EXISTS reward_failures (failure_id VARCHAR(129) PRIMARY KEY, delivery_id VARCHAR(129) NOT NULL, error_code VARCHAR(129) NOT NULL, retryable INTEGER NOT NULL, recorded_at BIGINT NOT NULL)",
        "CREATE TABLE IF NOT EXISTS progression_unlocks (player_id VARCHAR(36) NOT NULL, entitlement_id VARCHAR(129) NOT NULL, idempotency_key VARCHAR(129) NOT NULL UNIQUE, payload BLOB NOT NULL, PRIMARY KEY(player_id,entitlement_id))",
        "CREATE TABLE IF NOT EXISTS purchase_settlements (settlement_id VARCHAR(129) PRIMARY KEY, purchase_reference VARCHAR(129) NOT NULL UNIQUE, transaction_id VARCHAR(129) NOT NULL UNIQUE, state VARCHAR(24) NOT NULL, created_at BIGINT NOT NULL)"
    };
    private final int timeoutSeconds;

    /** Creates a migrator with a positive statement timeout. */
    public ProgressionSchemaMigrator(final int timeoutSeconds) {
        if (timeoutSeconds < 1) { throw new IllegalArgumentException("timeoutSeconds must be positive");}
        this.timeoutSeconds = timeoutSeconds;
    }
    /** @return canonical SHA-256 migration checksum */
    public String checksum() { return sha256(String.join("\n", SQL));}
    /** Applies the migration once and rejects checksum drift. */
    public Result<Report> migrate(final Connection connection) {
        if (connection == null) { throw new NullPointerException("connection");}
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
                statement.setString(2, "progression_persistence");
                statement.setString(3, checksum());
                statement.setLong(4, Instant.now().toEpochMilli());
                statement.executeUpdate();
            }
            return Result.success(new Report(true, checksum()));
        } catch (SQLException failure) { return Result.failure(SqlErrors.classify(failure));}
    }
    private String installed(final Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT checksum FROM zbw_schema_history WHERE version=?")) {
            statement.setQueryTimeout(timeoutSeconds);
            statement.setInt(1, VERSION);
            try (ResultSet result = statement.executeQuery()) { return result.next() ? result.getString(1) : null;}
        }
    }
    private static String sha256(final String input) {
        try {
            final byte[] digest = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
            final StringBuilder value = new StringBuilder(64);
            for (byte part : digest) { value.append(String.format("%02x", part & 0xff));}
            return value.toString();
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException("SHA-256 unavailable", impossible);}
    }
    /** Immutable migration evidence. */
    public static final class Report {
        private final boolean applied;
        private final String checksum;
        private Report(final boolean applied, final String checksum) { this.applied = applied;
        this.checksum = checksum;}
        /** @return whether this invocation applied M12 */ public boolean applied() { return applied;}
        /** @return installed checksum */ public String checksum() { return checksum;}
    }
}
