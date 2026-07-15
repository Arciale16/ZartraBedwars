package io.zartra.bedwars.storage.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.storage.api.RecordKey;
import io.zartra.bedwars.storage.api.RecordRevision;
import io.zartra.bedwars.storage.api.RecoveryService;
import io.zartra.bedwars.storage.api.StorageEngine.EngineKind;
import io.zartra.bedwars.storage.api.StoredRecord;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Unit and embedded tests for SQL configuration, migration, cache and recovery policy. */
final class SqlFoundationTest {
    private static final Instant NOW = Instant.parse("2026-07-15T00:00:00Z");
    private static final String CHECKSUM = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    @Test void configurationRedactsSecretsAndEnforcesTopology() {
        final char[] password = {'s', 'e', 'c', 'r', 'e', 't'};
        final SqlStorageConfiguration configuration = SqlStorageConfiguration.of(
                EngineKind.MYSQL, "jdbc:mysql://localhost/database", "user", password, 8,
                Duration.ofSeconds(5), Duration.ofSeconds(2));
        password[0] = 'x';
        assertEquals('s', configuration.password()[0]);
        assertFalse(configuration.toString().contains("secret"));
        assertEquals(8, configuration.maximumPoolSize());
        assertEquals(Duration.ofSeconds(5), configuration.connectionTimeout());
        assertEquals(Duration.ofSeconds(2), configuration.queryTimeout());
        SqlTopologyPolicy.validate(EngineKind.MARIADB, true);
        assertThrows(IllegalArgumentException.class,
                () -> SqlTopologyPolicy.validate(EngineKind.SQLITE, true));
        assertThrows(IllegalArgumentException.class, () -> SqlStorageConfiguration.of(
                EngineKind.SQLITE, "jdbc:mysql://wrong", "", new char[0], 1,
                Duration.ofSeconds(1), Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class, () -> SqlStorageConfiguration.of(
                EngineKind.SQLITE, "jdbc:sqlite:test", "", new char[0], 2,
                Duration.ofSeconds(1), Duration.ofSeconds(1)));
    }

    @Test void migrationIsOrderedIdempotentAndChecksumBacked() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            connection.setAutoCommit(false);
            final SchemaMigrator migrator = new SchemaMigrator(EngineKind.SQLITE, 2);
            assertEquals(1, migrator.plan().targetVersion());
            assertFalse(migrator.plan().requiresBackup());
            final SchemaMigrator.MigrationReport first = migrator.migrate(connection).requireValue();
            connection.commit();
            assertEquals(1, first.applied());
            assertFalse(first.alreadyCurrent());
            final SchemaMigrator.MigrationReport second = migrator.migrate(connection).requireValue();
            assertEquals(0, second.applied());
            assertTrue(second.alreadyCurrent());
            try (java.sql.ResultSet plan = connection.createStatement().executeQuery(
                    "EXPLAIN QUERY PLAN SELECT operation_id FROM zbw_outbox "
                            + "WHERE delivered_at IS NULL AND available_at <= 0 "
                            + "AND (claimed_until IS NULL OR claimed_until < 0) "
                            + "ORDER BY sequence_no, available_at, operation_id")) {
                boolean indexed = false;
                while (plan.next()) {
                    indexed |= plan.getString("detail").contains("idx_zbw_outbox_claim");
                }
                assertTrue(indexed);
            }
        }
        assertThrows(IllegalArgumentException.class, () -> new SchemaMigrator(EngineKind.SQLITE, 0));
        assertThrows(IllegalArgumentException.class,
                () -> SchemaMigrator.MigrationReport.of(0, 0, false));
    }

    @Test void cacheIsBoundedVersionAwareInvalidatableAndExpiring() throws Exception {
        final CaffeineVersionedCache cache = new CaffeineVersionedCache(2);
        final StoredRecord revisionTwo = record("one", 2);
        cache.put(revisionTwo, Duration.ofMinutes(1));
        assertTrue(cache.get(revisionTwo.key(), RecordRevision.of(2)).isPresent());
        assertFalse(cache.get(revisionTwo.key(), RecordRevision.of(3)).isPresent());
        cache.put(record("two", 1), Duration.ofMillis(5));
        Thread.sleep(20L);
        assertFalse(cache.get(record("two", 1).key(), RecordRevision.initial()).isPresent());
        cache.put(record("three", 1), Duration.ofMinutes(1));
        cache.invalidate(record("three", 1).key());
        assertEquals(Optional.empty(), cache.get(record("three", 1).key(), RecordRevision.initial()));
        cache.invalidateAll();
        assertEquals(0L, cache.estimatedEntries());
        assertEquals(2L, cache.maximumEntries());
        cache.close();
        assertThrows(IllegalArgumentException.class, () -> new CaffeineVersionedCache(0));
        assertThrows(IllegalArgumentException.class,
                () -> cache.put(record("bad", 1), Duration.ZERO));
    }

    @Test void recoveryRejectsUnencryptedEvidenceAndPreservesObjectives() {
        final RecoveryService.RecoveryObjectives objectives = RecoveryService.RecoveryObjectives.of(
                Duration.ZERO, Duration.ofMinutes(15));
        final SqlRecoveryCoordinator valid = new SqlRecoveryCoordinator(
                new EvidenceDriver(true, true), objectives);
        final DefinitionId backupId = DefinitionId.of("zartra", "nightly");
        assertTrue(valid.backup(backupId, NOW).isSuccess());
        assertTrue(valid.restore(backupId, NOW).isSuccess());
        assertEquals(objectives, valid.objectives());

        final SqlRecoveryCoordinator invalid = new SqlRecoveryCoordinator(
                new EvidenceDriver(false, true), objectives);
        assertTrue(invalid.backup(backupId, NOW).isFailure());
        assertTrue(invalid.restore(backupId, NOW).isFailure());
        final SqlRecoveryCoordinator failed = new SqlRecoveryCoordinator(
                new FailingDriver(), objectives);
        assertTrue(failed.backup(backupId, NOW).isFailure());
        assertTrue(failed.restore(backupId, NOW).isFailure());
        final SqlRecoveryCoordinator.BackupArtifact artifact =
                SqlRecoveryCoordinator.BackupArtifact.of(NOW, CHECKSUM, true, true);
        assertEquals(NOW, artifact.completedAt());
        assertEquals(CHECKSUM, artifact.checksum());
        assertTrue(artifact.encrypted());
        assertTrue(artifact.validated());
    }

    @Test void poolHealthDetectsSaturationAndRejectsImpossibleCounters() {
        assertTrue(PoolHealth.of(4, 0, 4, 2).saturated());
        assertFalse(PoolHealth.of(1, 2, 4, 0).saturated());
        assertEquals(2, PoolHealth.of(1, 2, 4, 0).idle());
        assertThrows(IllegalArgumentException.class, () -> PoolHealth.of(3, 2, 4, 0));
    }

    private static StoredRecord record(final String id, final long revision) {
        return StoredRecord.of(RecordKey.of(DefinitionId.of("zartra", "test"),
                DefinitionId.of("zartra", id)), RecordRevision.of(revision), 1,
                new byte[] {1, 2}, NOW);
    }

    private static final class EvidenceDriver implements SqlRecoveryCoordinator.BackupDriver {
        private final boolean encrypted;
        private final boolean validated;
        private EvidenceDriver(final boolean encrypted, final boolean validated) {
            this.encrypted = encrypted;
            this.validated = validated;
        }
        @Override public Result<SqlRecoveryCoordinator.BackupArtifact> createEncrypted(
                final DefinitionId backupId, final Instant requestedAt) {
            return Result.success(SqlRecoveryCoordinator.BackupArtifact.of(
                    requestedAt, CHECKSUM, encrypted, validated));
        }
        @Override public Result<SqlRecoveryCoordinator.BackupArtifact> validate(final DefinitionId backupId) {
            return Result.success(SqlRecoveryCoordinator.BackupArtifact.of(
                    NOW, CHECKSUM, encrypted, validated));
        }
        @Override public Result<SqlRecoveryCoordinator.BackupArtifact> restoreQuiescent(
                final DefinitionId backupId, final Instant requestedAt) {
            return createEncrypted(backupId, requestedAt);
        }
    }

    private static final class FailingDriver implements SqlRecoveryCoordinator.BackupDriver {
        @Override public Result<SqlRecoveryCoordinator.BackupArtifact> createEncrypted(
                final DefinitionId backupId, final Instant requestedAt) {
            return Result.failure(io.zartra.bedwars.api.result.ApiError.of(
                    DefinitionId.of("zartra", "failure"), "storage.error.failure",
                    io.zartra.bedwars.api.result.ApiError.RetryDisposition.RETRYABLE));
        }
        @Override public Result<SqlRecoveryCoordinator.BackupArtifact> validate(final DefinitionId backupId) {
            return createEncrypted(backupId, NOW);
        }
        @Override public Result<SqlRecoveryCoordinator.BackupArtifact> restoreQuiescent(
                final DefinitionId backupId, final Instant requestedAt) {
            return createEncrypted(backupId, requestedAt);
        }
    }
}
