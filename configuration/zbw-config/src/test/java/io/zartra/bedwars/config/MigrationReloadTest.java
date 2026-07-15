package io.zartra.bedwars.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.configuration.ConfigurationKey;
import io.zartra.bedwars.api.configuration.ConfigurationVersion;
import io.zartra.bedwars.api.configuration.ReloadTarget;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.config.migration.ConfigurationMigrationService;
import io.zartra.bedwars.config.migration.ConfigurationMigrationService.BackupReceipt;
import io.zartra.bedwars.config.migration.ConfigurationMigrationService.MigrationResult;
import io.zartra.bedwars.config.migration.ConfigurationMigrationService.Step;
import io.zartra.bedwars.config.reload.TransactionalReloadService;
import io.zartra.bedwars.config.reload.TransactionalReloadService.Participant;
import io.zartra.bedwars.config.reload.TransactionalReloadService.PreparedChange;
import io.zartra.bedwars.config.reload.TransactionalReloadService.ReloadReport;
import io.zartra.bedwars.config.reload.TransactionalReloadService.Status;
import io.zartra.bedwars.config.schema.ConfigurationModel.Document;
import io.zartra.bedwars.config.schema.ConfigurationModel.InitialCatalog;
import io.zartra.bedwars.config.schema.ConfigurationModel.LogicalFile;
import io.zartra.bedwars.config.schema.ConfigurationModel.Schema;
import io.zartra.bedwars.config.schema.ConfigurationModel.ValidatedConfiguration;
import io.zartra.bedwars.config.schema.ConfigurationModel.Validator;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class MigrationReloadTest {
    private final Validator validator = new Validator();

    @Test void migrationIsBackupFirstConsecutiveAndDeterministic() {
        final AtomicInteger backups = new AtomicInteger();
        final Step step = step(1, 2, false, true);
        final ConfigurationMigrationService service = new ConfigurationMigrationService(
                LogicalFile.CONFIG, Collections.singletonList(step));
        final Document source = Document.of(ConfigurationVersion.of(1),
                Collections.singletonMap(ConfigurationKey.of("old.key"), "value"));
        final MigrationResult result = service.migrate(source, ConfigurationVersion.of(2),
                (file, document) -> {
                    assertEquals(LogicalFile.CONFIG, file);
                    assertSame(source, document);
                    backups.incrementAndGet();
                    return BackupReceipt.of(DefinitionId.of("zartra", "backup/config-1"));
                });
        assertTrue(result.isSuccess());
        assertEquals(1, backups.get());
        assertSame(source, result.original());
        assertNotSame(source, result.result());
        assertEquals(ConfigurationVersion.of(2), result.result().version());
        assertEquals(Collections.singletonList(ConfigurationVersion.of(2)), result.appliedVersions());
        assertEquals("zartra:backup/config-1", result.backup().get().id().toString());
    }

    @Test void migrationFailureAlwaysReturnsOriginalDocument() {
        final Document source = Document.of(ConfigurationVersion.of(1),
                Collections.<ConfigurationKey, String>emptyMap());
        MigrationResult result = new ConfigurationMigrationService(LogicalFile.CONFIG,
                Collections.<Step>emptyList()).migrate(source, ConfigurationVersion.of(2),
                (file, document) -> BackupReceipt.of(DefinitionId.of("zartra", "backup/one")));
        assertFalse(result.isSuccess());
        assertSame(source, result.result());
        assertEquals("zartra:migration/step_missing", result.failure().get().toString());

        result = new ConfigurationMigrationService(LogicalFile.CONFIG,
                Collections.singletonList(step(1, 2, true, true)))
                .migrate(source, ConfigurationVersion.of(2),
                        (file, document) -> BackupReceipt.of(DefinitionId.of("zartra", "backup/two")));
        assertEquals("zartra:migration/step_failed", result.failure().get().toString());
        assertSame(source, result.result());

        result = new ConfigurationMigrationService(LogicalFile.CONFIG,
                Collections.singletonList(step(1, 2, false, true)))
                .migrate(source, ConfigurationVersion.of(2), (file, document) -> {
                    throw new IllegalStateException("disk unavailable");
                });
        assertEquals("zartra:migration/backup_failed", result.failure().get().toString());
        assertSame(source, result.result());

        final Document newer = source.atVersion(ConfigurationVersion.of(3));
        result = new ConfigurationMigrationService(LogicalFile.CONFIG,
                Collections.singletonList(step(1, 2, false, true)))
                .migrate(newer, ConfigurationVersion.of(2),
                        (file, document) -> BackupReceipt.of(DefinitionId.of("zartra", "backup/three")));
        assertEquals("zartra:migration/downgrade_forbidden", result.failure().get().toString());
    }

    @Test void migrationPlanRejectsGapsDuplicatesAndVersionContractViolations() {
        assertThrows(IllegalArgumentException.class, () -> new ConfigurationMigrationService(
                LogicalFile.CONFIG, Collections.singletonList(step(1, 3, false, true))));
        assertThrows(IllegalArgumentException.class, () -> new ConfigurationMigrationService(
                LogicalFile.CONFIG, Arrays.asList(step(1, 2, false, true),
                        step(1, 2, false, true))));
        final Document source = Document.of(ConfigurationVersion.of(1),
                Collections.<ConfigurationKey, String>emptyMap());
        final MigrationResult badVersion = new ConfigurationMigrationService(LogicalFile.CONFIG,
                Collections.singletonList(step(1, 2, false, false)))
                .migrate(source, ConfigurationVersion.of(2),
                        (file, document) -> BackupReceipt.of(DefinitionId.of("zartra", "backup/four")));
        assertEquals("zartra:migration/version_contract", badVersion.failure().get().toString());
        final MigrationResult unchanged = new ConfigurationMigrationService(LogicalFile.CONFIG,
                Collections.<Step>emptyList()).migrate(source, ConfigurationVersion.of(1),
                (file, document) -> BackupReceipt.of(DefinitionId.of("zartra", "backup/unused")));
        assertTrue(unchanged.isSuccess());
        assertFalse(unchanged.backup().isPresent());
    }

    @Test void targetedReloadPublishesOnlyAfterEveryParticipantApplies() {
        final Schema schema = InitialCatalog.schema(LogicalFile.MESSAGES);
        final ValidatedConfiguration active = valid(schema, empty());
        final TrackingParticipant participant = new TrackingParticipant(false, false, false);
        final TransactionalReloadService service = new TransactionalReloadService(validator, active,
                Collections.<Participant>singletonList(participant));
        final ReloadReport report = service.reload(document("catalog.default-locale", "it-IT"),
                Collections.singleton(ReloadTarget.MESSAGES));
        assertEquals(Status.APPLIED, report.status());
        assertTrue(report.wasApplied());
        assertEquals(1, participant.prepared.get());
        assertEquals(1, participant.applied.get());
        assertEquals(0, participant.rolledBack.get());
        assertNotSame(active, service.active());
    }

    @Test void reloadRejectsValidationRestartTargetAndNoChangeCases() {
        final Schema messages = InitialCatalog.schema(LogicalFile.MESSAGES);
        final ValidatedConfiguration active = valid(messages, empty());
        final TransactionalReloadService service = new TransactionalReloadService(validator, active,
                Collections.<Participant>emptyList());
        assertEquals(Status.NO_CHANGES, service.reload(empty(),
                Collections.singleton(ReloadTarget.MESSAGES)).status());
        assertEquals(Status.VALIDATION_FAILED, service.reload(document("unknown.key", "x"),
                Collections.singleton(ReloadTarget.MESSAGES)).status());
        assertEquals(Status.TARGET_MISMATCH, service.reload(document("catalog.default-locale", "it-IT"),
                Collections.singleton(ReloadTarget.GUI)).status());
        assertSame(active, service.active());

        final Schema deployment = InitialCatalog.schema(LogicalFile.DEPLOYMENT);
        final TransactionalReloadService restart = new TransactionalReloadService(validator,
                valid(deployment, empty()), Collections.<Participant>emptyList());
        final ReloadReport restartReport = restart.reload(document("deployment.mode", "scalable-proxy"),
                Collections.singleton(ReloadTarget.CORE));
        assertEquals(Status.RESTART_REQUIRED, restartReport.status());
        assertTrue(restartReport.restartRequiredKeys().contains(ConfigurationKey.of("deployment.mode")));
        assertThrows(IllegalArgumentException.class, () -> service.reload(empty(),
                Collections.<ReloadTarget>emptySet()));
    }

    @Test void prepareAndApplyFailuresRollBackAndKeepLastKnownGood() {
        final Schema schema = InitialCatalog.schema(LogicalFile.MESSAGES);
        final ValidatedConfiguration active = valid(schema, empty());
        final TrackingParticipant prepareFail = new TrackingParticipant(true, false, false);
        TransactionalReloadService service = new TransactionalReloadService(validator, active,
                Collections.<Participant>singletonList(prepareFail));
        assertEquals(Status.PREPARE_FAILED, service.reload(document("catalog.default-locale", "it-IT"),
                Collections.singleton(ReloadTarget.MESSAGES)).status());
        assertSame(active, service.active());

        final TrackingParticipant applyFail = new TrackingParticipant(false, true, false);
        service = new TransactionalReloadService(validator, active,
                Collections.<Participant>singletonList(applyFail));
        final ReloadReport failed = service.reload(document("catalog.default-locale", "it-IT"),
                Collections.singleton(ReloadTarget.MESSAGES));
        assertEquals(Status.APPLY_FAILED, failed.status());
        assertEquals("zartra:reload/apply_failed", failed.failure().get().toString());
        assertEquals(1, applyFail.rolledBack.get());
        assertSame(active, service.active());

        final TrackingParticipant rollbackFail = new TrackingParticipant(false, true, true);
        service = new TransactionalReloadService(validator, active,
                Collections.<Participant>singletonList(rollbackFail));
        final ReloadReport rollback = service.reload(document("catalog.default-locale", "it-IT"),
                Collections.singleton(ReloadTarget.MESSAGES));
        assertEquals("zartra:reload/rollback_failed", rollback.failure().get().toString());
        assertSame(active, service.active());
        assertThrows(IllegalArgumentException.class, () -> new TransactionalReloadService(validator,
                active, Arrays.<Participant>asList(applyFail, rollbackFail)));
    }

    private static Step step(final int from, final int to, final boolean fail,
                             final boolean honorVersion) {
        return new Step() {
            @Override public ConfigurationVersion from() { return ConfigurationVersion.of(from); }
            @Override public ConfigurationVersion to() { return ConfigurationVersion.of(to); }
            @Override public Document migrate(final Document source) {
                if (fail) { throw new IllegalStateException("migration failed"); }
                return source.atVersion(ConfigurationVersion.of(honorVersion ? to : from));
            }
        };
    }
    private ValidatedConfiguration valid(final Schema schema, final Document document) {
        return validator.validate(schema, document).configuration().get();
    }
    private static Document empty() {
        return Document.of(ConfigurationVersion.of(1), Collections.<ConfigurationKey, String>emptyMap());
    }
    private static Document document(final String key, final String value) {
        final Map<ConfigurationKey, String> values = new HashMap<ConfigurationKey, String>();
        values.put(ConfigurationKey.of(key), value);
        return Document.of(ConfigurationVersion.of(1), values);
    }

    private static final class TrackingParticipant implements Participant {
        private final boolean failPrepare;
        private final boolean failApply;
        private final boolean failRollback;
        private final AtomicInteger prepared = new AtomicInteger();
        private final AtomicInteger applied = new AtomicInteger();
        private final AtomicInteger rolledBack = new AtomicInteger();
        private TrackingParticipant(final boolean failPrepare, final boolean failApply,
                                    final boolean failRollback) {
            this.failPrepare = failPrepare;
            this.failApply = failApply;
            this.failRollback = failRollback;
        }
        @Override public ReloadTarget target() { return ReloadTarget.MESSAGES; }
        @Override public PreparedChange prepare(final ValidatedConfiguration current,
                                                final ValidatedConfiguration candidate) {
            prepared.incrementAndGet();
            if (failPrepare) { throw new IllegalStateException("prepare failed"); }
            return new PreparedChange() {
                @Override public void apply() {
                    applied.incrementAndGet();
                    if (failApply) { throw new IllegalStateException("apply failed"); }
                }
                @Override public void rollback() {
                    rolledBack.incrementAndGet();
                    if (failRollback) { throw new IllegalStateException("rollback failed"); }
                }
            };
        }
    }
}
