package io.zartra.bedwars.storage.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.storage.api.RecordKey;
import io.zartra.bedwars.storage.api.RecordRevision;
import io.zartra.bedwars.storage.api.StorageEngine.EngineKind;
import io.zartra.bedwars.storage.api.StoredRecord;
import io.zartra.bedwars.storage.api.TransactionOptions;
import io.zartra.bedwars.storage.api.UnitOfWork;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.SQLiteDataSource;

/** Exercises bounded failure, malformed-input and retry paths required by M04. */
final class SqlBranchAndFailureTest {
    @Test void sqlErrorsClassifyVendorStatesWithoutLeakingMessages() {
        final io.zartra.bedwars.api.result.ApiError conflict =
                SqlErrors.classify(new SQLException("duplicate", "23000"));
        assertTrue(SqlErrors.classify(new SQLException("secret", "08001")).retryDisposition()
                == io.zartra.bedwars.api.result.ApiError.RetryDisposition.RETRYABLE);
        assertTrue(SqlErrors.classify(new SQLException("secret", "40001")).retryDisposition()
                == io.zartra.bedwars.api.result.ApiError.RetryDisposition.RETRYABLE);
        assertEquals(conflict.messageKey(), SqlErrors.classify(new SQLException("duplicate", "23000"))
                .messageKey());
        assertEquals(conflict.retryDisposition(), SqlErrors.classify(new SQLException("duplicate", "23000"))
                .retryDisposition());
        assertFalse(SqlErrors.classify(new SQLException("fatal", "42000")).messageKey().contains("fatal"));
        assertFalse(SqlErrors.classify(new SQLException("duplicate", null, 1205)).messageKey()
                .equals(conflict.messageKey()));
        assertFalse(SqlErrors.duplicate(new SQLException("duplicate", null, 1205)));
        assertTrue(SqlErrors.duplicate(new SQLException("duplicate", "23000")));
        assertTrue(SqlErrors.duplicate(new SQLException("duplicate", null, 1062)));
        assertTrue(SqlErrors.duplicate(new SQLException("duplicate", null, 19)));
        assertFalse(SqlErrors.duplicate(new SQLException("other", "42000", 1)));
    }

    @Test void supportRetriesOnlyBoundedDeadlocks() throws Exception {
        final JdbcStorageEngine engine = memoryEngine();
        final JdbcUnitOfWork retrying = (JdbcUnitOfWork) engine.begin(TransactionOptions.of(
                TransactionOptions.AccessMode.READ_WRITE, Duration.ofMillis(1001), 2)).requireValue();
        final AtomicInteger attempts = new AtomicInteger();
        final Result<String> recovered = JdbcSupport.execute(retrying,
                new JdbcSupport.SqlOperation<String>() {
                    @Override public Result<String> execute() throws SQLException {
                        if (attempts.getAndIncrement() == 0) { throw new SQLException("deadlock", "40001"); }
                        return Result.success("ok");
                    }
                });
        assertEquals("ok", recovered.requireValue());
        assertEquals(2, attempts.get());
        assertEquals(2, JdbcSupport.timeoutSeconds(retrying));
        retrying.rollback();

        final JdbcUnitOfWork noRetry = (JdbcUnitOfWork) engine.begin(TransactionOptions.of(
                TransactionOptions.AccessMode.READ_WRITE, Duration.ofMillis(1), 0)).requireValue();
        assertTrue(JdbcSupport.execute(noRetry, new JdbcSupport.SqlOperation<String>() {
            @Override public Result<String> execute() throws SQLException {
                throw new SQLException("deadlock", "40P01");
            }
        }).isFailure());
        noRetry.rollback();
        engine.close();
    }

    @Test void configurationAndCacheRejectEveryUnsafeBoundary() {
        for (EngineKind kind : new EngineKind[] {EngineKind.MYSQL, EngineKind.MARIADB}) {
            final String url = kind == EngineKind.MYSQL ? "jdbc:mysql://host/db" : "jdbc:mariadb://host/db";
            final SqlStorageConfiguration value = SqlStorageConfiguration.of(kind, url, null, null,
                    1, Duration.ofSeconds(1), Duration.ofSeconds(1));
            assertEquals("", value.username());
            assertEquals(0, value.password().length);
        }
        assertThrows(IllegalArgumentException.class, () -> SqlStorageConfiguration.of(
                EngineKind.MYSQL, "jdbc:mysql://host/db\npassword=x", "", null, 1,
                Duration.ofSeconds(1), Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class, () -> SqlStorageConfiguration.of(
                EngineKind.MYSQL, "jdbc:mysql://host/db", "", null, 0,
                Duration.ofSeconds(1), Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class, () -> SqlStorageConfiguration.of(
                EngineKind.MYSQL, "jdbc:mysql://host/db", "", null, 65,
                Duration.ofSeconds(1), Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class, () -> SqlStorageConfiguration.of(
                EngineKind.MYSQL, "jdbc:mysql://host/db", "", null, 1,
                null, Duration.ofSeconds(1)));

        final CaffeineVersionedCache cache = new CaffeineVersionedCache(1);
        assertThrows(NullPointerException.class,
                () -> cache.get(null, RecordRevision.initial()));
        assertThrows(NullPointerException.class, () -> cache.invalidate(null));
        assertThrows(IllegalArgumentException.class, () -> cache.put(record("x"),
                Duration.ofSeconds(Long.MAX_VALUE)));
        assertThrows(IllegalArgumentException.class, () -> new CaffeineVersionedCache(10_000_001L));
    }

    @Test void migrationDetectsChecksumDriftAndUnsafeExternalDdl() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            connection.setAutoCommit(false);
            final SchemaMigrator sqlite = new SchemaMigrator(EngineKind.SQLITE, 1);
            sqlite.migrate(connection).requireValue();
            connection.commit();
            connection.createStatement().executeUpdate(
                    "UPDATE zbw_schema_history SET checksum = "
                            + "'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb'");
            assertTrue(sqlite.migrate(connection).isFailure());
        }
        assertTrue(new SchemaMigrator(EngineKind.MYSQL, 1).plan().requiresBackup());
        assertTrue(new SchemaMigrator(EngineKind.MARIADB, 1).plan().requiresBackup());
        final SchemaMigrator.MigrationReport report = SchemaMigrator.MigrationReport.of(2, 1, false);
        assertEquals(2, report.version());
        assertFalse(report.alreadyCurrent());
        assertThrows(IllegalArgumentException.class,
                () -> SchemaMigrator.MigrationReport.of(1, 2, false));
    }

    @Test void statisticsSchemaMigrationDetectsChecksumDrift() throws Exception {
        final JdbcStorageEngine engine = memoryEngine();
        try (UnitOfWork unit = engine.begin(TransactionOptions.of(
                TransactionOptions.AccessMode.READ_WRITE, Duration.ofSeconds(1), 0)).requireValue()) {
            final JdbcUnitOfWork jdbc = (JdbcUnitOfWork) unit;
            final StatisticsSchemaMigrator migrator = new StatisticsSchemaMigrator(5);
            migrator.migrate(jdbc.connection()).requireValue();
            jdbc.connection().createStatement().executeUpdate(
                    "UPDATE zbw_schema_history SET "
                            + "checksum='cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc' "
                            + "WHERE version = 15");
            assertTrue(migrator.migrate(jdbc.connection()).isFailure());
            unit.rollback().requireValue();
        }
        engine.close();
    }

    @Test void flywayBridgeRunsApprovedClasspathMigration(final @TempDir Path temporary) throws Exception {
        final SQLiteDataSource source = new SQLiteDataSource();
        source.setUrl("jdbc:sqlite:" + temporary.resolve("flyway.db").toAbsolutePath());
        assertThrows(IllegalArgumentException.class,
                () -> FlywayMigrationProvider.migrate(source, "file:/unsafe"));
        final Result<Boolean> migration = FlywayMigrationProvider.migrate(
                source, "classpath:db/migration/sqlite");
        final String specification = System.getProperty("java.specification.version");
        final int feature = specification.startsWith("1.")
                ? Integer.parseInt(specification.substring(2)) : Integer.parseInt(specification);
        if (feature >= 17) {
            assertTrue(migration.isSuccess());
            try (Connection connection = source.getConnection()) {
                assertTrue(connection.getMetaData().getTables(
                        null, null, "zbw_flyway_contract", null).next());
            }
        } else {
            assertTrue(migration.isFailure());
        }
    }

    @Test void engineAndUnitsRejectClosedInvalidAndForeignOperations() {
        assertTrue(JdbcStorageEngine.open(SqlStorageConfiguration.of(EngineKind.MYSQL,
                "jdbc:mysql://127.0.0.1:1/unreachable", "x", new char[] {'x'}, 1,
                Duration.ofMillis(250), Duration.ofSeconds(1))).isFailure());
        final JdbcStorageEngine engine = memoryEngine();
        assertThrows(NullPointerException.class, () -> engine.begin(null));
        final UnitOfWork read = engine.begin(TransactionOptions.of(
                TransactionOptions.AccessMode.READ_ONLY, Duration.ofSeconds(1), 0)).requireValue();
        assertThrows(IllegalStateException.class, () -> engine.records().save(read,
                record("read-only"), RecordRevision.initial()));
        assertTrue(read.rollback().isSuccess());
        assertTrue(read.rollback().isFailure());
        read.close();
        engine.close();
        engine.close();
    }

    @Test void repositoryAndMessageBoundsRejectMalformedCalls() {
        final JdbcStorageEngine engine = memoryEngine();
        final UnitOfWork write = engine.begin(TransactionOptions.of(
                TransactionOptions.AccessMode.READ_WRITE, Duration.ofSeconds(1), 0)).requireValue();
        assertThrows(NullPointerException.class, () -> engine.records().find(write, null));
        assertThrows(NullPointerException.class, () -> engine.records().save(write, null,
                RecordRevision.initial()));
        assertThrows(NullPointerException.class, () -> engine.records().delete(write, key("x"), null));
        assertThrows(IllegalArgumentException.class, () -> engine.messages().claim(write,
                Instant.now(), 0, Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class, () -> engine.messages().claim(write,
                Instant.now(), 1001, Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class, () -> engine.messages().claim(write,
                Instant.now(), 1, Duration.ZERO));
        assertThrows(NullPointerException.class, () -> engine.messages().enqueue(write, null));
        assertThrows(NullPointerException.class, () -> engine.messages().acknowledge(write, null));
        assertThrows(NullPointerException.class, () -> engine.messages().receive(write, null));
        assertThrows(NullPointerException.class, () -> SqlTopologyPolicy.validate(null, true));
        assertThrows(
                IllegalArgumentException.class,
                () -> SqlTopologyPolicy.validate(
                        io.zartra.bedwars.storage.api.StorageEngine.EngineKind.SQLITE,
                        true));
        write.rollback();
        engine.close();
    }

    @Test void poolHealthExposesEverySanitizedCounter() {
        final PoolHealth health = PoolHealth.of(1, 2, 4, 3);
        assertEquals(1, health.active());
        assertEquals(2, health.idle());
        assertEquals(4, health.total());
        assertEquals(3, health.waiting());
        assertThrows(IllegalArgumentException.class, () -> PoolHealth.of(-1, 0, 0, 0));
    }

    private static JdbcStorageEngine memoryEngine() {
        return JdbcStorageEngine.open(SqlStorageConfiguration.of(EngineKind.SQLITE,
                "jdbc:sqlite::memory:", "", new char[0], 1, Duration.ofSeconds(1),
                Duration.ofSeconds(1))).requireValue();
    }
    private static RecordKey key(final String id) {
        return RecordKey.of(DefinitionId.of("zartra", "failure"), DefinitionId.of("zartra", id));
    }
    private static StoredRecord record(final String id) {
        return StoredRecord.of(key(id), RecordRevision.initial(), 1, new byte[] {1}, Instant.EPOCH);
    }
}
