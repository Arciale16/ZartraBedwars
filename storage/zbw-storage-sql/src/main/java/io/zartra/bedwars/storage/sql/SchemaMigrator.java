package io.zartra.bedwars.storage.sql;

import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.storage.api.Migration;
import io.zartra.bedwars.storage.api.MigrationPlan;
import io.zartra.bedwars.storage.api.StorageEngine.EngineKind;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Collections;

/** Deterministic schema runner with Flyway-compatible version/checksum history semantics. */
public final class SchemaMigrator {
    private static final String DESCRIPTION = "storage_foundation";
    private final EngineKind kind;
    private final int queryTimeoutSeconds;

    /** Creates a migrator for one SQL dialect. */
    public SchemaMigrator(final EngineKind kind, final int queryTimeoutSeconds) {
        if (kind == null) { throw new NullPointerException("kind"); }
        if (queryTimeoutSeconds < 1) { throw new IllegalArgumentException("queryTimeoutSeconds must be positive"); }
        this.kind = kind;
        this.queryTimeoutSeconds = queryTimeoutSeconds;
    }

    /** @return canonical one-step M04 migration plan */
    public MigrationPlan plan() {
        final String canonical = String.join("\n", statements(kind));
        return MigrationPlan.of(Collections.singletonList(Migration.of(
                1, DESCRIPTION, sha256(canonical), kind != EngineKind.SQLITE)));
    }

    /** Applies missing ordered migrations and rejects checksum drift. */
    public Result<MigrationReport> migrate(final Connection connection) {
        if (connection == null) { throw new NullPointerException("connection"); }
        final Migration migration = plan().migrations().get(0);
        try {
            createHistory(connection);
            final String installed = installedChecksum(connection, migration.version());
            if (installed != null) {
                if (!installed.equals(migration.checksum())) { return Result.failure(SqlErrors.CONFLICT); }
                return Result.success(MigrationReport.of(migration.version(), 0, true));
            }
            for (String sql : statements(kind)) {
                if (sql.startsWith("CREATE INDEX ") && indexExists(connection, "idx_zbw_outbox_claim")) {
                    continue;
                }
                try (Statement statement = connection.createStatement()) {
                    statement.setQueryTimeout(queryTimeoutSeconds);
                    statement.execute(sql);
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO zbw_schema_history(version, description, checksum, installed_at) VALUES(?, ?, ?, ?)")) {
                statement.setQueryTimeout(queryTimeoutSeconds);
                statement.setInt(1, migration.version());
                statement.setString(2, migration.description());
                statement.setString(3, migration.checksum());
                statement.setLong(4, Instant.now().toEpochMilli());
                statement.executeUpdate();
            }
            return Result.success(MigrationReport.of(migration.version(), 1, false));
        } catch (SQLException exception) {
            return Result.failure(SqlErrors.classify(exception));
        }
    }

    private static boolean indexExists(final Connection connection, final String name) throws SQLException {
        final DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet indexes = metadata.getIndexInfo(null, null, "zbw_outbox", false, false)) {
            while (indexes.next()) {
                if (name.equalsIgnoreCase(indexes.getString("INDEX_NAME"))) { return true; }
            }
        }
        return false;
    }

    private void createHistory(final Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(queryTimeoutSeconds);
            statement.execute("CREATE TABLE IF NOT EXISTS zbw_schema_history ("
                    + "version INTEGER NOT NULL PRIMARY KEY, description VARCHAR(64) NOT NULL, "
                    + "checksum CHAR(64) NOT NULL, installed_at BIGINT NOT NULL)");
        }
    }

    private String installedChecksum(final Connection connection, final int version) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT checksum FROM zbw_schema_history WHERE version = ?")) {
            statement.setQueryTimeout(queryTimeoutSeconds);
            statement.setInt(1, version);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getString(1) : null;
            }
        }
    }

    private static String[] statements(final EngineKind kind) {
        final String outboxIndex = kind == EngineKind.SQLITE
                ? "CREATE INDEX IF NOT EXISTS idx_zbw_outbox_claim ON zbw_outbox(delivered_at, available_at, claimed_until, sequence_no)"
                : "CREATE INDEX idx_zbw_outbox_claim ON zbw_outbox(delivered_at, available_at, claimed_until, sequence_no)";
        return new String[] {
            "CREATE TABLE IF NOT EXISTS zbw_records (aggregate_type VARCHAR(129) NOT NULL, aggregate_id VARCHAR(129) NOT NULL, revision BIGINT NOT NULL, schema_version INTEGER NOT NULL, payload BLOB NOT NULL, updated_at BIGINT NOT NULL, PRIMARY KEY (aggregate_type, aggregate_id))",
            "CREATE TABLE IF NOT EXISTS zbw_outbox (operation_id VARCHAR(129) NOT NULL PRIMARY KEY, event_id VARCHAR(36) NOT NULL, event_type VARCHAR(129) NOT NULL, correlation_id VARCHAR(36) NOT NULL, occurred_at BIGINT NOT NULL, sequence_no BIGINT NOT NULL, schema_version INTEGER NOT NULL, thread_context VARCHAR(32) NOT NULL, payload BLOB NOT NULL, available_at BIGINT NOT NULL, claimed_until BIGINT NULL, delivered_at BIGINT NULL)",
            outboxIndex,
            "CREATE TABLE IF NOT EXISTS zbw_inbox (operation_id VARCHAR(129) NOT NULL PRIMARY KEY, event_id VARCHAR(36) NOT NULL, received_at BIGINT NOT NULL)",
            "CREATE TABLE IF NOT EXISTS zbw_retention (aggregate_type VARCHAR(129) NOT NULL, aggregate_id VARCHAR(129) NOT NULL, retention_class VARCHAR(129) NOT NULL, expires_at BIGINT NOT NULL, deletion_deadline_ms BIGINT NOT NULL, PRIMARY KEY (aggregate_type, aggregate_id))",
            "CREATE TABLE IF NOT EXISTS zbw_legal_hold (case_id VARCHAR(36) NOT NULL PRIMARY KEY, aggregate_type VARCHAR(129) NOT NULL, aggregate_id VARCHAR(129) NOT NULL, authorized_by VARCHAR(36) NOT NULL, placed_at BIGINT NOT NULL, released_at BIGINT NULL, released_by VARCHAR(36) NULL)",
            "CREATE TABLE IF NOT EXISTS zbw_tombstone (aggregate_type VARCHAR(129) NOT NULL, aggregate_id VARCHAR(129) NOT NULL, deleted_at BIGINT NOT NULL, PRIMARY KEY (aggregate_type, aggregate_id))",
            "CREATE TABLE IF NOT EXISTS zbw_backup_history (backup_id VARCHAR(129) NOT NULL PRIMARY KEY, operation VARCHAR(16) NOT NULL, completed_at BIGINT NOT NULL, checksum CHAR(64) NOT NULL)"
        };
    }

    private static String sha256(final String value) {
        try {
            final byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            final StringBuilder result = new StringBuilder(64);
            for (byte part : digest) { result.append(String.format("%02x", part & 0xff)); }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    /** Immutable migration execution evidence. */
    public static final class MigrationReport {
        private final int version;
        private final int applied;
        private final boolean alreadyCurrent;
        private MigrationReport(final int version, final int applied, final boolean alreadyCurrent) {
            this.version = version;
            this.applied = applied;
            this.alreadyCurrent = alreadyCurrent;
        }
        /** @return execution report */
        public static MigrationReport of(final int version, final int applied,
                                         final boolean alreadyCurrent) {
            if (version < 1 || applied < 0 || applied > version) {
                throw new IllegalArgumentException("invalid migration report");
            }
            return new MigrationReport(version, applied, alreadyCurrent);
        }
        /** @return resulting schema version */ public int version() { return version; }
        /** @return migrations applied in this run */ public int applied() { return applied; }
        /** @return whether no mutation was necessary */ public boolean alreadyCurrent() { return alreadyCurrent; }
    }
}
