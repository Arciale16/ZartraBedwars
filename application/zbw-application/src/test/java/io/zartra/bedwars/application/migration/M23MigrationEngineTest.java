package io.zartra.bedwars.application.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.migration.MigrationApi;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class M23MigrationEngineTest {
    @Test
    void dryRunIsDeterministicAndDoesNotMutateOrBackup() {
        final Fixture fixture = new Fixture();
        final MigrationApi.Request request = request("migration/dry", MigrationApi.Mode.DRY_RUN,
                MigrationApi.ConflictPolicy.FAIL, record("b"), record("a"));

        final MigrationApi.Plan plan = fixture.engine.plan(request).toCompletableFuture().join();
        final MigrationApi.Report report = fixture.engine.execute(request).toCompletableFuture().join();

        assertTrue(plan.applicable());
        assertEquals(Arrays.asList("target/a", "target/b"),
                Arrays.asList(plan.targetRecords().get(0).id(), plan.targetRecords().get(1).id()));
        assertEquals(MigrationApi.Status.PLANNED, report.status());
        assertTrue(fixture.target.records.isEmpty());
        assertTrue(fixture.backups.values.isEmpty());
    }

    @Test
    void applyCreatesBackupAndRollbackRestoresSnapshot() {
        final Fixture fixture = new Fixture();
        fixture.target.records.add(target("target/existing"));
        final MigrationApi.Request request = request("migration/apply", MigrationApi.Mode.APPLY,
                MigrationApi.ConflictPolicy.REPLACE, record("existing"), record("new"));

        final MigrationApi.Report applied =
                fixture.engine.execute(request).toCompletableFuture().join();
        final MigrationApi.Report rolledBack =
                fixture.engine.rollback(request.migrationId()).toCompletableFuture().join();

        assertEquals(MigrationApi.Status.APPLIED, applied.status());
        assertEquals(2, fixture.backups.values.get("backup/migration/apply").size() + 1);
        assertEquals(MigrationApi.Status.ROLLED_BACK, rolledBack.status());
        assertEquals(Collections.singletonList("target/existing"),
                Collections.singletonList(fixture.target.records.get(0).id()));
    }

    @Test
    void rejectsUnsupportedDuplicateAndExistingTargets() {
        final Fixture fixture = new Fixture();
        fixture.target.records.add(target("target/a"));
        final MigrationApi.Request existing = request("migration/existing",
                MigrationApi.Mode.APPLY, MigrationApi.ConflictPolicy.FAIL, record("a"));
        final MigrationApi.Request unsupported = new MigrationApi.Request("migration/unsupported",
                "operator-export", "operator-authorized", MigrationApi.Mode.APPLY,
                MigrationApi.ConflictPolicy.FAIL,
                Collections.singletonList(new MigrationApi.Record(
                        "unknown", "unknown-kind", Collections.<String, String>emptyMap())));
        final MigrationApi.Request duplicate = request("migration/duplicate",
                MigrationApi.Mode.APPLY, MigrationApi.ConflictPolicy.FAIL,
                record("same"), record("same"));

        assertFalse(fixture.engine.plan(existing).toCompletableFuture().join().applicable());
        assertEquals(MigrationApi.Status.REJECTED,
                fixture.engine.execute(unsupported).toCompletableFuture().join().status());
        assertFalse(fixture.engine.plan(duplicate).toCompletableFuture().join().applicable());
        assertTrue(fixture.backups.values.isEmpty());
    }

    @Test
    void keepExistingOmitsConflictAndFailedApplyRestoresBackup() {
        final Fixture fixture = new Fixture();
        fixture.target.records.add(target("target/a"));
        final MigrationApi.Plan kept = fixture.engine.plan(request("migration/keep",
                MigrationApi.Mode.APPLY, MigrationApi.ConflictPolicy.KEEP_EXISTING,
                record("a"), record("b"))).toCompletableFuture().join();
        assertTrue(kept.applicable());
        assertEquals(Collections.singletonList("target/b"),
                Collections.singletonList(kept.targetRecords().get(0).id()));

        fixture.target.fail = true;
        final MigrationApi.Report failed = fixture.engine.execute(request("migration/fail",
                MigrationApi.Mode.APPLY, MigrationApi.ConflictPolicy.REPLACE,
                record("b"))).toCompletableFuture().join();
        assertEquals(MigrationApi.Status.FAILED, failed.status());
        assertEquals(Collections.singletonList("target/a"),
                Collections.singletonList(fixture.target.records.get(0).id()));
        assertEquals(MigrationApi.Status.REJECTED,
                fixture.engine.rollback("migration/missing").toCompletableFuture().join().status());
    }

    @Test
    void rejectsInvalidBackupBeforeJournalOrMutation() {
        final MemoryTarget target = new MemoryTarget();
        final MemoryJournal journal = new MemoryJournal();
        final DeterministicMigrationEngine engine = new DeterministicMigrationEngine(
                Collections.singletonList(new MappingProvider("provider/test")), target,
                new DeterministicMigrationEngine.BackupStore() {
                    @Override public String capture(final String migrationId,
                                                    final List<MigrationApi.Record> snapshot) {
                        return " ";
                    }
                    @Override public void restore(final String backupId,
                                                  final DeterministicMigrationEngine.Target owner) {
                        throw new AssertionError("restore must not run");
                    }
                }, journal, Runnable::run);

        assertThrows(java.util.concurrent.CompletionException.class, () -> engine.execute(
                request("migration/invalid-backup", MigrationApi.Mode.APPLY,
                        MigrationApi.ConflictPolicy.FAIL, record("a")))
                .toCompletableFuture().join());
        assertTrue(target.records.isEmpty());
        assertTrue(journal.entries.isEmpty());
    }

    @Test
    void rejectsDuplicateAndAmbiguousProviders() {
        final MigrationApi.Provider first = new MappingProvider("provider/a");
        assertThrows(IllegalArgumentException.class, () -> new DeterministicMigrationEngine(
                Arrays.asList(first, first), new MemoryTarget(), new MemoryBackups(),
                new MemoryJournal(), Runnable::run));

        final DeterministicMigrationEngine ambiguous = new DeterministicMigrationEngine(
                Arrays.asList(first, new MappingProvider("provider/b")), new MemoryTarget(),
                new MemoryBackups(), new MemoryJournal(), Runnable::run);
        assertThrows(java.util.concurrent.CompletionException.class,
                () -> ambiguous.plan(request("migration/ambiguous", MigrationApi.Mode.DRY_RUN,
                        MigrationApi.ConflictPolicy.FAIL, record("a")))
                        .toCompletableFuture().join());
    }

    private static MigrationApi.Request request(final String id, final MigrationApi.Mode mode,
                                                final MigrationApi.ConflictPolicy policy,
                                                final MigrationApi.Record... records) {
        return new MigrationApi.Request(id, "operator-export", "operator-authorized", mode,
                policy, Arrays.asList(records));
    }

    private static MigrationApi.Record record(final String id) {
        return new MigrationApi.Record(id, "test-kind", Collections.<String, String>emptyMap());
    }

    private static MigrationApi.Record target(final String id) {
        return new MigrationApi.Record(id, "target-kind", Collections.<String, String>emptyMap());
    }

    private static final class Fixture {
        private final MemoryTarget target = new MemoryTarget();
        private final MemoryBackups backups = new MemoryBackups();
        private final MemoryJournal journal = new MemoryJournal();
        private final DeterministicMigrationEngine engine = new DeterministicMigrationEngine(
                Collections.singletonList(new MappingProvider("provider/test")),
                target, backups, journal, Runnable::run);
    }

    private static final class MappingProvider implements MigrationApi.Provider {
        private final String id;
        MappingProvider(final String id) { this.id = id; }
        @Override public String id() { return id; }
        @Override public boolean supports(final String kind) { return "test-kind".equals(kind); }
        @Override public MigrationApi.Conversion convert(final MigrationApi.Record source) {
            return new MigrationApi.Conversion(MigrationApi.ConversionState.MAPPED,
                    Collections.singletonList(target("target/" + source.id())), "mapped");
        }
    }

    private static final class MemoryTarget implements DeterministicMigrationEngine.Target {
        private final List<MigrationApi.Record> records = new ArrayList<MigrationApi.Record>();
        private boolean fail;
        @Override public List<MigrationApi.Record> snapshot() {
            return new ArrayList<MigrationApi.Record>(records);
        }
        @Override public Set<String> ids() {
            final Set<String> ids = new HashSet<String>();
            for (MigrationApi.Record record : records) { ids.add(record.id()); }
            return ids;
        }
        @Override public void apply(final List<MigrationApi.Record> values,
                                    final MigrationApi.ConflictPolicy policy) {
            if (fail) { throw new IllegalStateException("simulated"); }
            for (MigrationApi.Record value : values) {
                records.removeIf(existing -> existing.id().equals(value.id()));
                records.add(value);
            }
            Collections.sort(records);
        }
        @Override public void restore(final List<MigrationApi.Record> snapshot) {
            records.clear();
            records.addAll(snapshot);
        }
    }

    private static final class MemoryBackups
            implements DeterministicMigrationEngine.BackupStore {
        private final Map<String, List<MigrationApi.Record>> values =
                new HashMap<String, List<MigrationApi.Record>>();
        @Override public String capture(final String migrationId,
                                        final List<MigrationApi.Record> snapshot) {
            final String id = "backup/" + migrationId;
            values.put(id, new ArrayList<MigrationApi.Record>(snapshot));
            return id;
        }
        @Override public void restore(final String backupId,
                                      final DeterministicMigrationEngine.Target target) {
            target.restore(values.get(backupId));
        }
    }

    private static final class MemoryJournal implements DeterministicMigrationEngine.Journal {
        private final Map<String, MutableEntry> entries = new HashMap<String, MutableEntry>();
        @Override public void prepared(final String migrationId, final String backupId,
                                       final MigrationApi.Plan plan) {
            entries.put(migrationId, new MutableEntry(backupId));
        }
        @Override public void applied(final String migrationId) {
            entries.get(migrationId).applied = true;
        }
        @Override public void failed(final String migrationId, final String failureType) {
            entries.get(migrationId).applied = false;
        }
        @Override public void rolledBack(final String migrationId) {
            entries.get(migrationId).applied = false;
        }
        @Override public Entry entry(final String migrationId) { return entries.get(migrationId); }
    }

    private static final class MutableEntry implements DeterministicMigrationEngine.Journal.Entry {
        private final String backupId;
        private boolean applied;
        MutableEntry(final String backupId) { this.backupId = backupId; }
        @Override public String backupId() { return backupId; }
        @Override public boolean applied() { return applied; }
    }
}
