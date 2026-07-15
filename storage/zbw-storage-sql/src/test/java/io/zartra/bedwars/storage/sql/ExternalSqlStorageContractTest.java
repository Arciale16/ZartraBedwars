package io.zartra.bedwars.storage.sql;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.event.EventMetadata;
import io.zartra.bedwars.api.identity.CaseId;
import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.EventId;
import io.zartra.bedwars.api.identity.EventTypeId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.storage.api.MessageEnvelope;
import io.zartra.bedwars.storage.api.RecordKey;
import io.zartra.bedwars.storage.api.RecordRevision;
import io.zartra.bedwars.storage.api.RecoveryService;
import io.zartra.bedwars.storage.api.RetentionPolicy;
import io.zartra.bedwars.storage.api.StorageEngine.EngineKind;
import io.zartra.bedwars.storage.api.StoredRecord;
import io.zartra.bedwars.storage.api.TransactionOptions;
import io.zartra.bedwars.storage.api.UnitOfWork;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/** Required real-MySQL or real-MariaDB M04 certification suite with sanitized evidence. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
final class ExternalSqlStorageContractTest {
    private static final Instant NOW = Instant.parse("2026-07-15T00:00:00Z");
    private static final int MAXIMUM_POOL_SIZE = 4;
    private JdbcDatabaseContainer<?> container;
    private JdbcStorageEngine engine;
    private EngineKind kind;
    private String engineName;
    private String imageReference;
    private String expectedVersion;
    private String username;
    private String password;
    private Path evidenceDirectory;

    @BeforeAll void startCertifiedDatabase() throws Exception {
        engineName = requiredEnvironment("ZBW_TEST_DATABASE_ENGINE");
        imageReference = requiredEnvironment("ZBW_TEST_DATABASE_IMAGE");
        expectedVersion = requiredEnvironment("ZBW_TEST_DATABASE_VERSION");
        username = requiredEnvironment("ZBW_TEST_DATABASE_USERNAME");
        password = requiredEnvironment("ZBW_TEST_DATABASE_PASSWORD");
        final String databaseName = requiredEnvironment("ZBW_TEST_DATABASE_NAME");
        evidenceDirectory = Paths.get(requiredEnvironment("ZBW_M04_EVIDENCE_DIR"))
                .toAbsolutePath().normalize();
        assertTrue(imageReference.matches(
                "docker\\.io/library/(mysql|mariadb):[0-9]+(?:\\.[0-9]+)+@sha256:[0-9a-f]{64}"));
        requireDocker();
        final DockerImageName image = DockerImageName.parse(imageReference)
                .asCompatibleSubstituteFor(engineName);
        if ("mysql".equals(engineName)) {
            kind = EngineKind.MYSQL;
            container = new MySQLContainer<>(image);
        } else if ("mariadb".equals(engineName)) {
            kind = EngineKind.MARIADB;
            container = new MariaDBContainer<>(image);
        } else {
            throw new AssertionError("unsupported certified database engine: " + engineName);
        }
        container.withDatabaseName(databaseName).withUsername(username).withPassword(password);
        container.start();
        engine = openEngine();
        Files.createDirectories(evidenceDirectory);
    }

    @AfterAll void stopCertifiedDatabase() {
        if (engine != null) {
            engine.close();
        }
        if (container != null) {
            container.stop();
        }
        password = null;
    }

    @Test @Order(1) void serverIdentityMatchesLockedImageVersion() throws Exception {
        final String actualVersion = serverVersion();
        assertTrue(actualVersion.equals(expectedVersion)
                || actualVersion.startsWith(expectedVersion + "-")
                || actualVersion.startsWith(expectedVersion + "+"));
        writeEvidence("database-identity.json", "{\n"
                + "  \"engine\": " + json(engineName) + ",\n"
                + "  \"server_version\": " + json(actualVersion) + ",\n"
                + "  \"image_reference\": " + json(imageReference) + ",\n"
                + "  \"status\": \"CERTIFIED\"\n"
                + "}\n");
    }

    @Test @Order(2) void repositoryContractsUsePreparedStatementsAndUniqueConstraints() {
        final RecordKey key = key("repository");
        final byte[] payload = "' OR 1=1; DROP TABLE zbw_records; --"
                .getBytes(StandardCharsets.UTF_8);
        try (UnitOfWork transaction = write(engine)) {
            final StoredRecord saved = engine.records().save(transaction,
                    StoredRecord.of(key, RecordRevision.initial(), 1, payload, NOW),
                    RecordRevision.initial()).requireValue();
            assertEquals(1L, saved.revision().value());
            assertTrue(engine.records().save(transaction, record(key, 9),
                    RecordRevision.initial()).isFailure());
            assertTrue(engine.messages().enqueue(transaction, envelope("unique", NOW)).requireValue());
            assertFalse(engine.messages().enqueue(transaction, envelope("unique", NOW)).requireValue());
            transaction.commit().requireValue();
        }
        try (UnitOfWork read = read(engine)) {
            final StoredRecord stored = engine.records().find(read, key).requireValue().get();
            assertArrayEquals(payload, stored.payload());
            read.rollback().requireValue();
        }
    }

    @Test @Order(3) void migrationsAndChecksumValidationAreDeterministic() throws Exception {
        final String expectedChecksum = new SchemaMigrator(kind, 3)
                .plan().migrations().get(0).checksum();
        try (Connection connection = rawConnection()) {
            assertEquals(8, tableCount(connection));
            try (PreparedStatement query = connection.prepareStatement(
                    "SELECT checksum FROM zbw_schema_history WHERE version = ?")) {
                query.setInt(1, 1);
                try (ResultSet result = query.executeQuery()) {
                    assertTrue(result.next());
                    assertEquals(expectedChecksum, result.getString(1));
                }
            }
            try (PreparedStatement corrupt = connection.prepareStatement(
                    "UPDATE zbw_schema_history SET checksum = ? WHERE version = ?")) {
                corrupt.setString(1, repeat('0', 64));
                corrupt.setInt(2, 1);
                assertEquals(1, corrupt.executeUpdate());
            }
        }
        try {
            assertTrue(openEngineResult().isFailure());
        } finally {
            try (Connection connection = rawConnection();
                 PreparedStatement repair = connection.prepareStatement(
                         "UPDATE zbw_schema_history SET checksum = ? WHERE version = ?")) {
                repair.setString(1, expectedChecksum);
                repair.setInt(2, 1);
                assertEquals(1, repair.executeUpdate());
            }
        }
        final JdbcStorageEngine restarted = openEngine();
        restarted.close();
    }

    @Test @Order(4) void transactionRollbackSurvivesConnectionReuse() {
        final RecordKey key = key("rollback");
        try (UnitOfWork abandoned = write(engine)) {
            assertTrue(engine.records().save(abandoned, record(key, 1),
                    RecordRevision.initial()).isSuccess());
        }
        try (UnitOfWork read = read(engine)) {
            assertFalse(engine.records().find(read, key).requireValue().isPresent());
            read.rollback().requireValue();
        }
    }

    @Test @Order(5) void concurrentWritersPreserveOptimisticConstraint() throws Exception {
        final RecordKey key = key("concurrent");
        try (UnitOfWork transaction = write(engine)) {
            engine.records().save(transaction, record(key, 1), RecordRevision.initial()).requireValue();
            transaction.commit().requireValue();
        }
        final CyclicBarrier barrier = new CyclicBarrier(2);
        final ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            final Future<Boolean> left = workers.submit(
                    () -> concurrentUpdate(key, barrier, 2));
            final Future<Boolean> right = workers.submit(
                    () -> concurrentUpdate(key, barrier, 3));
            final int successes = (left.get() ? 1 : 0) + (right.get() ? 1 : 0);
            assertEquals(1, successes);
        } finally {
            workers.shutdownNow();
        }
    }

    @Test @Order(6) void deadlockRetryIsBoundedAroundARealConnection() throws Exception {
        final AtomicInteger attempts = new AtomicInteger();
        try (UnitOfWork transaction = write(engine)) {
            final JdbcUnitOfWork jdbc = (JdbcUnitOfWork) transaction;
            final Result<String> recovered = JdbcSupport.execute(jdbc, () -> {
                if (attempts.getAndIncrement() == 0) {
                    throw new SQLException("deterministic deadlock fault", "40001", 1213);
                }
                try (PreparedStatement statement = jdbc.connection().prepareStatement(
                        "SELECT VERSION()"); ResultSet result = statement.executeQuery()) {
                    assertTrue(result.next());
                    return Result.success(result.getString(1));
                }
            });
            assertTrue(recovered.isSuccess());
            assertEquals(2, attempts.get());
            transaction.rollback().requireValue();
        }
        final AtomicInteger exhaustedAttempts = new AtomicInteger();
        try (UnitOfWork transaction = engine.begin(options(
                TransactionOptions.AccessMode.READ_WRITE, 2)).requireValue()) {
            final Result<Boolean> exhausted = JdbcSupport.execute((JdbcUnitOfWork) transaction, () -> {
                exhaustedAttempts.incrementAndGet();
                throw new SQLException("deterministic deadlock fault", "40001", 1213);
            });
            assertTrue(exhausted.isFailure());
            assertEquals(3, exhaustedAttempts.get());
            transaction.rollback().requireValue();
        }
    }

    @Test @Order(7) void queryTimeoutCancelsLongRunningPreparedStatement() throws Exception {
        final long started = System.nanoTime();
        try (UnitOfWork transaction = engine.begin(TransactionOptions.of(
                TransactionOptions.AccessMode.READ_ONLY, Duration.ofSeconds(1), 0)).requireValue()) {
            final JdbcUnitOfWork jdbc = (JdbcUnitOfWork) transaction;
            try (PreparedStatement statement = jdbc.connection().prepareStatement("SELECT SLEEP(?)")) {
                statement.setInt(1, 2);
                statement.setQueryTimeout(1);
                assertThrows(SQLException.class, statement::executeQuery);
            }
        } finally {
            final long elapsedMillis = Duration.ofNanos(System.nanoTime() - started).toMillis();
            assertTrue(elapsedMillis >= 500L && elapsedMillis < 5000L);
            restartEngine();
        }
    }

    @Test @Order(8) void outboxInboxCrashAndDuplicateRecoveryAreDurable() {
        final MessageEnvelope envelope = envelope("crash-recovery", NOW);
        try (UnitOfWork abandoned = write(engine)) {
            assertTrue(engine.messages().enqueue(abandoned, envelope).requireValue());
            assertTrue(engine.messages().receive(abandoned, envelope).requireValue());
        }
        assertEquals(0, count("zbw_outbox", "operation_id", envelope.operationId().toString()));
        assertEquals(0, count("zbw_inbox", "operation_id", envelope.operationId().toString()));
        try (UnitOfWork committed = write(engine)) {
            assertTrue(engine.messages().enqueue(committed, envelope).requireValue());
            assertFalse(engine.messages().enqueue(committed, envelope).requireValue());
            assertTrue(engine.messages().receive(committed, envelope).requireValue());
            assertFalse(engine.messages().receive(committed, envelope).requireValue());
            committed.commit().requireValue();
        }
        try (UnitOfWork claim = write(engine)) {
            assertEquals(1, engine.messages().claim(claim, NOW.plusSeconds(1), 1,
                    Duration.ofSeconds(1)).requireValue().size());
            claim.commit().requireValue();
        }
        restartEngine();
        try (UnitOfWork recover = write(engine)) {
            assertEquals(1, engine.messages().claim(recover, NOW.plusSeconds(3), 1,
                    Duration.ofSeconds(1)).requireValue().size());
            assertTrue(engine.messages().acknowledge(recover, envelope.operationId()).requireValue());
            assertFalse(engine.messages().acknowledge(recover, envelope.operationId()).requireValue());
            recover.commit().requireValue();
        }
    }

    @Test @Order(9) void backupAndRestoreRoundTripUsesValidatedEncryptedEvidence() throws Exception {
        final RecordKey key = key("backup");
        try (UnitOfWork transaction = write(engine)) {
            engine.records().save(transaction, record(key, 7), RecordRevision.initial()).requireValue();
            transaction.commit().requireValue();
        }
        final SnapshotBackupDriver driver = new SnapshotBackupDriver(container, key, password);
        final SqlRecoveryCoordinator recovery = new SqlRecoveryCoordinator(driver,
                RecoveryService.RecoveryObjectives.of(Duration.ZERO, Duration.ofMinutes(15)));
        final DefinitionId backupId = DefinitionId.of("zartra", "m04-ci-backup");
        final RecoveryService.BackupEvidence backup = recovery.backup(backupId, NOW).requireValue();
        try (UnitOfWork transaction = write(engine)) {
            assertTrue(engine.records().delete(transaction, key, RecordRevision.of(1)).requireValue());
            transaction.commit().requireValue();
        }
        assertFalse(find(key).isPresent());
        final RecoveryService.BackupEvidence restored = recovery.restore(
                backupId, NOW.plusSeconds(1)).requireValue();
        assertEquals(backup.checksum(), restored.checksum());
        assertEquals(7, find(key).get().payload()[0]);
        writeEvidence("backup-restore.json", "{\n"
                + "  \"engine\": " + json(engineName) + ",\n"
                + "  \"sha256\": " + json(backup.checksum()) + ",\n"
                + "  \"encrypted\": true,\n"
                + "  \"validated_before_restore\": true,\n"
                + "  \"status\": \"CERTIFIED\"\n"
                + "}\n");
    }

    @Test @Order(10) void retentionLegalHoldAndTombstonePersistAcrossRestart() {
        final RecordKey key = key("privacy");
        final CaseId caseId = CaseId.of(UUID.fromString("10000000-0000-0000-0000-000000000010"));
        final PlayerId operator = PlayerId.of(UUID.fromString("20000000-0000-0000-0000-000000000020"));
        final RetentionPolicy policy = RetentionPolicy.of(
                DefinitionId.of("zartra", "reported-replay"), Duration.ofDays(90), Duration.ofDays(30));
        try (UnitOfWork transaction = write(engine)) {
            assertTrue(engine.retention().retain(
                    transaction, key, policy, NOW.plus(Duration.ofDays(90))).requireValue());
            assertTrue(engine.retention().hold(transaction, caseId, key, operator, NOW).requireValue());
            assertFalse(engine.retention().hold(transaction, caseId, key, operator, NOW).requireValue());
            assertTrue(engine.retention().release(
                    transaction, caseId, operator, NOW.plusSeconds(1)).requireValue());
            assertTrue(engine.retention().tombstone(transaction, key, NOW.plusSeconds(2)).requireValue());
            assertFalse(engine.retention().tombstone(transaction, key, NOW.plusSeconds(3)).requireValue());
            transaction.commit().requireValue();
        }
        restartEngine();
        assertEquals(1, count("zbw_retention", "aggregate_id", key.aggregateId().toString()));
        assertEquals(1, count("zbw_legal_hold", "case_id", caseId.toString()));
        assertEquals(1, count("zbw_tombstone", "aggregate_id", key.aggregateId().toString()));
    }

    @Test @Order(11) void representativeQueryPlansUseCertifiedIndexes() throws Exception {
        seedQueryPlanFixture();
        final Map<String, Plan> plans = new LinkedHashMap<>();
        plans.put("record-primary-key", explain(
                "SELECT revision, schema_version, payload, updated_at FROM zbw_records "
                        + "WHERE aggregate_type = ? AND aggregate_id = ?",
                new Object[] {key("repository").aggregateType().toString(),
                    key("repository").aggregateId().toString()}, "PRIMARY"));
        plans.put("outbox-claim-index", explain(
                "SELECT operation_id FROM zbw_outbox WHERE delivered_at IS NULL "
                        + "AND available_at <= ? AND (claimed_until IS NULL OR claimed_until < ?) "
                        + "ORDER BY sequence_no, available_at, operation_id",
                new Object[] {NOW.toEpochMilli(), NOW.toEpochMilli()}, "idx_zbw_outbox_claim"));
        plans.put("inbox-primary-key", explain(
                "SELECT event_id FROM zbw_inbox WHERE operation_id = ?",
                new Object[] {envelope("crash-recovery", NOW).operationId().toString()}, "PRIMARY"));
        plans.put("retention-primary-key", explain(
                "SELECT expires_at FROM zbw_retention WHERE aggregate_type = ? AND aggregate_id = ?",
                new Object[] {key("privacy").aggregateType().toString(),
                    key("privacy").aggregateId().toString()}, "PRIMARY"));
        plans.put("legal-hold-primary-key", explain(
                "SELECT released_at FROM zbw_legal_hold WHERE case_id = ?",
                new Object[] {"10000000-0000-0000-0000-000000000010"}, "PRIMARY"));
        plans.put("tombstone-primary-key", explain(
                "SELECT deleted_at FROM zbw_tombstone WHERE aggregate_type = ? AND aggregate_id = ?",
                new Object[] {key("privacy").aggregateType().toString(),
                    key("privacy").aggregateId().toString()}, "PRIMARY"));
        plans.put("migration-history-primary-key", explain(
                "SELECT checksum FROM zbw_schema_history WHERE version = ?",
                new Object[] {1}, "PRIMARY"));
        final StringBuilder evidence = new StringBuilder("{\n  \"engine\": ")
                .append(json(engineName)).append(",\n  \"fixture_rows\": 2064,\n  \"queries\": [\n");
        int index = 0;
        for (Map.Entry<String, Plan> entry : plans.entrySet()) {
            if (index++ > 0) {
                evidence.append(",\n");
            }
            evidence.append("    {\"id\": ").append(json(entry.getKey()))
                    .append(", \"expected_index\": ").append(json(entry.getValue().expectedIndex))
                    .append(", \"uses_expected_index\": true, \"full_table_access\": false")
                    .append(", \"explain_json\": ").append(json(entry.getValue().json)).append('}');
        }
        evidence.append("\n  ],\n  \"status\": \"CERTIFIED\"\n}\n");
        writeEvidence("query-plans.json", evidence.toString());
    }

    @Test @Order(12) void hikariPoolHealthEvidenceIsBoundedAndCredentialFree() throws Exception {
        final PoolHealth health = engine.poolHealth();
        assertTrue(health.active() >= 0);
        assertTrue(health.idle() >= 0);
        assertTrue(health.total() <= MAXIMUM_POOL_SIZE);
        assertTrue(health.waiting() >= 0);
        final boolean saturated = health.total() == MAXIMUM_POOL_SIZE && health.waiting() > 0;
        final String evidence = "{\n"
                + "  \"engine\": " + json(engineName) + ",\n"
                + "  \"active\": " + health.active() + ",\n"
                + "  \"idle\": " + health.idle() + ",\n"
                + "  \"total\": " + health.total() + ",\n"
                + "  \"waiting\": " + health.waiting() + ",\n"
                + "  \"maximum\": " + MAXIMUM_POOL_SIZE + ",\n"
                + "  \"saturated\": " + saturated + ",\n"
                + "  \"status\": \"CERTIFIED\"\n"
                + "}\n";
        assertFalse(evidence.contains(password));
        assertFalse(evidence.contains(username));
        assertFalse(evidence.contains("jdbc:"));
        writeEvidence("pool-health.json", evidence);
    }

    private boolean concurrentUpdate(final RecordKey key, final CyclicBarrier barrier,
                                     final int payload) throws Exception {
        try (UnitOfWork transaction = write(engine)) {
            barrier.await();
            final Result<StoredRecord> result = engine.records().save(
                    transaction, record(key, payload), RecordRevision.of(1));
            if (result.isSuccess()) {
                transaction.commit().requireValue();
                return true;
            }
            transaction.rollback().requireValue();
            return false;
        }
    }

    private Plan explain(final String sql, final Object[] parameters,
                         final String expectedIndex) throws Exception {
        try (Connection connection = rawConnection();
             PreparedStatement statement = connection.prepareStatement("EXPLAIN FORMAT=JSON " + sql)) {
            for (int index = 0; index < parameters.length; index++) {
                statement.setObject(index + 1, parameters[index]);
            }
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                final String plan = result.getString(1);
                final String compact = plan.replaceAll("\\s+", "");
                assertTrue(compact.contains("\"key\":\"" + expectedIndex + "\""), plan);
                assertFalse(compact.contains("\"access_type\":\"ALL\""), plan);
                return new Plan(expectedIndex, plan);
            }
        }
    }

    private void seedQueryPlanFixture() throws Exception {
        try (Connection connection = rawConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement delete = connection.prepareStatement(
                    "DELETE FROM zbw_outbox WHERE operation_id LIKE ?")) {
                delete.setString(1, "zartra:plan-%");
                delete.executeUpdate();
            }
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO zbw_outbox(operation_id, event_id, event_type, correlation_id, "
                            + "occurred_at, sequence_no, schema_version, thread_context, payload, "
                            + "available_at, claimed_until, delivered_at) "
                            + "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, ?)")) {
                for (int index = 0; index < 2064; index++) {
                    insert.setString(1, String.format("zartra:plan-%04d", index));
                    insert.setString(2, UUID.nameUUIDFromBytes(
                            ("plan-" + index).getBytes(StandardCharsets.UTF_8)).toString());
                    insert.setString(3, "zartra:query-plan");
                    insert.setString(4, "30000000-0000-0000-0000-000000000030");
                    insert.setLong(5, NOW.toEpochMilli());
                    insert.setLong(6, 10000L + index);
                    insert.setInt(7, 1);
                    insert.setString(8, EventMetadata.ThreadContext.APPLICATION_WORKER.name());
                    insert.setBytes(9, new byte[] {1});
                    insert.setLong(10, index < 2048 ? NOW.plusSeconds(3600).toEpochMilli()
                            : NOW.minusSeconds(1).toEpochMilli());
                    if (index < 2048) {
                        insert.setLong(11, NOW.toEpochMilli());
                    } else {
                        insert.setNull(11, java.sql.Types.BIGINT);
                    }
                    insert.addBatch();
                }
                assertEquals(2064, insert.executeBatch().length);
            }
            connection.commit();
        }
    }

    private int tableCount(final Connection connection) throws SQLException {
        int count = 0;
        final String[] tables = {"zbw_records", "zbw_outbox", "zbw_inbox", "zbw_retention",
            "zbw_legal_hold", "zbw_tombstone", "zbw_backup_history", "zbw_schema_history"};
        for (String table : tables) {
            try (ResultSet result = connection.getMetaData().getTables(
                    connection.getCatalog(), null, table, new String[] {"TABLE"})) {
                if (result.next()) {
                    count++;
                }
            }
        }
        return count;
    }

    private int count(final String table, final String column, final String value) {
        final String sql = "SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?";
        try (Connection connection = rawConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                return result.getInt(1);
            }
        } catch (SQLException exception) {
            throw new AssertionError("count query failed", exception);
        }
    }

    private java.util.Optional<StoredRecord> find(final RecordKey key) {
        try (UnitOfWork read = read(engine)) {
            final java.util.Optional<StoredRecord> result = engine.records().find(read, key).requireValue();
            read.rollback().requireValue();
            return result;
        }
    }

    private JdbcStorageEngine openEngine() {
        return openEngineResult().requireValue();
    }

    private Result<JdbcStorageEngine> openEngineResult() {
        return JdbcStorageEngine.open(SqlStorageConfiguration.of(
                kind, container.getJdbcUrl(), container.getUsername(),
                container.getPassword().toCharArray(), MAXIMUM_POOL_SIZE,
                Duration.ofSeconds(10), Duration.ofSeconds(3)));
    }

    private void restartEngine() {
        engine.close();
        engine = openEngine();
    }

    private Connection rawConnection() throws SQLException {
        return container.createConnection("");
    }

    private String serverVersion() throws SQLException {
        try (Connection connection = rawConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT VERSION()");
             ResultSet result = statement.executeQuery()) {
            assertTrue(result.next());
            return result.getString(1);
        }
    }

    private void writeEvidence(final String name, final String content) throws Exception {
        Files.write(evidenceDirectory.resolve(name), content.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
    }

    private static String requiredEnvironment(final String name) {
        final String value = System.getenv(name);
        if (value != null && !value.trim().isEmpty()) {
            return value;
        }
        if ("1".equals(System.getenv("ZBW_REQUIRE_EXTERNAL_DATABASES"))) {
            throw new AssertionError(name + " is mandatory in the external database workflow");
        }
        Assumptions.assumeTrue(false, name + " is absent outside the external database workflow");
        return "";
    }

    private static void requireDocker() {
        if (DockerClientFactory.instance().isDockerAvailable()) {
            return;
        }
        if ("1".equals(System.getenv("ZBW_REQUIRE_EXTERNAL_DATABASES"))) {
            throw new AssertionError("Docker is mandatory in the external database workflow");
        }
        Assumptions.assumeTrue(false, "Docker is absent outside the external database workflow");
    }

    private static UnitOfWork write(final JdbcStorageEngine storage) {
        return storage.begin(options(TransactionOptions.AccessMode.READ_WRITE, 3)).requireValue();
    }

    private static UnitOfWork read(final JdbcStorageEngine storage) {
        return storage.begin(options(TransactionOptions.AccessMode.READ_ONLY, 0)).requireValue();
    }

    private static TransactionOptions options(final TransactionOptions.AccessMode mode,
                                              final int retries) {
        return TransactionOptions.of(mode, Duration.ofSeconds(3), retries);
    }

    private static RecordKey key(final String id) {
        return RecordKey.of(DefinitionId.of("zartra", "external-contract"),
                DefinitionId.of("zartra", id));
    }

    private static StoredRecord record(final RecordKey key, final int payload) {
        return StoredRecord.of(key, RecordRevision.initial(), 1,
                new byte[] {(byte) payload}, NOW);
    }

    private static MessageEnvelope envelope(final String id, final Instant availableAt) {
        return MessageEnvelope.of(IdempotencyKey.of("zartra", id), EventMetadata.of(
                EventId.of(UUID.nameUUIDFromBytes(id.getBytes(StandardCharsets.UTF_8))),
                EventTypeId.of("zartra", "external-contract"),
                CorrelationId.of(UUID.fromString("40000000-0000-0000-0000-000000000040")),
                NOW, Math.abs(id.hashCode()) + 1L, 1,
                EventMetadata.ThreadContext.APPLICATION_WORKER), new byte[] {2}, availableAt);
    }

    private static String repeat(final char value, final int count) {
        final char[] characters = new char[count];
        java.util.Arrays.fill(characters, value);
        return new String(characters);
    }

    private static String json(final String value) {
        return '"' + value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", "\\r").replace("\n", "\\n") + '"';
    }

    private static final class Plan {
        private final String expectedIndex;
        private final String json;
        private Plan(final String expectedIndex, final String json) {
            this.expectedIndex = expectedIndex;
            this.json = json;
        }
    }

    private static final class SnapshotBackupDriver implements SqlRecoveryCoordinator.BackupDriver {
        private static final int NONCE_LENGTH = 12;
        private final JdbcDatabaseContainer<?> container;
        private final RecordKey key;
        private final byte[] encryptionKey;
        private byte[] encryptedSnapshot;
        private String checksum;

        SnapshotBackupDriver(final JdbcDatabaseContainer<?> container,
                             final RecordKey key, final String secret) throws Exception {
            this.container = container;
            this.key = key;
            this.encryptionKey = MessageDigest.getInstance("SHA-256")
                    .digest(secret.getBytes(StandardCharsets.UTF_8));
        }

        @Override public Result<SqlRecoveryCoordinator.BackupArtifact> createEncrypted(
                final DefinitionId backupId, final Instant requestedAt) {
            try {
                encryptedSnapshot = encrypt(readRecord());
                checksum = hexadecimal(MessageDigest.getInstance("SHA-256").digest(encryptedSnapshot));
                try (Connection connection = container.createConnection("");
                     PreparedStatement statement = connection.prepareStatement(
                             "INSERT INTO zbw_backup_history(backup_id, operation, completed_at, checksum) "
                                     + "VALUES(?, ?, ?, ?)")) {
                    statement.setString(1, backupId.toString());
                    statement.setString(2, "BACKUP");
                    statement.setLong(3, requestedAt.toEpochMilli());
                    statement.setString(4, checksum);
                    assertEquals(1, statement.executeUpdate());
                }
                return artifact(requestedAt, validateSnapshot());
            } catch (Exception exception) {
                return Result.failure(SqlErrors.CONFLICT);
            }
        }

        @Override public Result<SqlRecoveryCoordinator.BackupArtifact> validate(
                final DefinitionId backupId) {
            try {
                return artifact(NOW, validateSnapshot() && backupExists(backupId));
            } catch (Exception exception) {
                return Result.failure(SqlErrors.CONFLICT);
            }
        }

        @Override public Result<SqlRecoveryCoordinator.BackupArtifact> restoreQuiescent(
                final DefinitionId backupId, final Instant requestedAt) {
            try {
                restoreRecord(decrypt(encryptedSnapshot));
                return artifact(requestedAt, validateSnapshot() && backupExists(backupId));
            } catch (Exception exception) {
                return Result.failure(SqlErrors.CONFLICT);
            }
        }

        private Result<SqlRecoveryCoordinator.BackupArtifact> artifact(
                final Instant completedAt, final boolean valid) {
            return Result.success(SqlRecoveryCoordinator.BackupArtifact.of(
                    completedAt, checksum, true, valid));
        }

        private byte[] readRecord() throws Exception {
            try (Connection connection = container.createConnection("");
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT revision, schema_version, payload, updated_at FROM zbw_records "
                                 + "WHERE aggregate_type = ? AND aggregate_id = ?")) {
                bindKey(statement);
                try (ResultSet result = statement.executeQuery()) {
                    assertTrue(result.next());
                    final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    try (DataOutputStream data = new DataOutputStream(bytes)) {
                        data.writeLong(result.getLong(1));
                        data.writeInt(result.getInt(2));
                        final byte[] payload = result.getBytes(3);
                        data.writeInt(payload.length);
                        data.write(payload);
                        data.writeLong(result.getLong(4));
                    }
                    return bytes.toByteArray();
                }
            }
        }

        private void restoreRecord(final byte[] snapshot) throws Exception {
            try (DataInputStream data = new DataInputStream(new ByteArrayInputStream(snapshot));
                 Connection connection = container.createConnection("")) {
                connection.setAutoCommit(false);
                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM zbw_records WHERE aggregate_type = ? AND aggregate_id = ?")) {
                    bindKey(delete);
                    delete.executeUpdate();
                }
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO zbw_records(aggregate_type, aggregate_id, revision, "
                                + "schema_version, payload, updated_at) VALUES(?, ?, ?, ?, ?, ?)")) {
                    bindKey(insert);
                    insert.setLong(3, data.readLong());
                    insert.setInt(4, data.readInt());
                    final byte[] payload = new byte[data.readInt()];
                    data.readFully(payload);
                    insert.setBytes(5, payload);
                    insert.setLong(6, data.readLong());
                    assertEquals(1, insert.executeUpdate());
                }
                connection.commit();
            }
        }

        private byte[] encrypt(final byte[] plaintext) throws Exception {
            final byte[] nonce = new byte[NONCE_LENGTH];
            new SecureRandom().nextBytes(nonce);
            final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(encryptionKey, "AES"),
                    new GCMParameterSpec(128, nonce));
            final ByteArrayOutputStream result = new ByteArrayOutputStream();
            result.write(nonce);
            result.write(cipher.doFinal(plaintext));
            return result.toByteArray();
        }

        private byte[] decrypt(final byte[] encrypted) throws Exception {
            final byte[] nonce = java.util.Arrays.copyOfRange(encrypted, 0, NONCE_LENGTH);
            final byte[] ciphertext = java.util.Arrays.copyOfRange(
                    encrypted, NONCE_LENGTH, encrypted.length);
            final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(encryptionKey, "AES"),
                    new GCMParameterSpec(128, nonce));
            return cipher.doFinal(ciphertext);
        }

        private boolean validateSnapshot() throws Exception {
            return encryptedSnapshot != null
                    && checksum.equals(hexadecimal(
                            MessageDigest.getInstance("SHA-256").digest(encryptedSnapshot)))
                    && decrypt(encryptedSnapshot).length > 0;
        }

        private boolean backupExists(final DefinitionId backupId) throws SQLException {
            try (Connection connection = container.createConnection("");
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT checksum FROM zbw_backup_history WHERE backup_id = ?")) {
                statement.setString(1, backupId.toString());
                try (ResultSet result = statement.executeQuery()) {
                    return result.next() && checksum.equals(result.getString(1));
                }
            }
        }

        private void bindKey(final PreparedStatement statement) throws SQLException {
            statement.setString(1, key.aggregateType().toString());
            statement.setString(2, key.aggregateId().toString());
        }

        private static String hexadecimal(final byte[] bytes) {
            final StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                result.append(String.format("%02x", value & 0xff));
            }
            return result.toString();
        }
    }
}
