package io.zartra.bedwars.application.migration;

import io.zartra.bedwars.api.migration.MigrationApi;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

/**
 * Deterministic migration orchestrator with mandatory backup, journal and rollback boundaries.
 *
 * <p>The supplied executor must be bounded. Target operations are required to be atomic; the engine
 * restores the captured snapshot when an apply operation fails. It owns no domain data.</p>
 */
public final class DeterministicMigrationEngine implements MigrationApi.Service {
    private final List<MigrationApi.Provider> providers;
    private final Target target;
    private final BackupStore backups;
    private final Journal journal;
    private final Executor executor;

    /** Creates an engine over injected atomic ports. */
    public DeterministicMigrationEngine(final Collection<? extends MigrationApi.Provider> providers,
                                        final Target target, final BackupStore backups,
                                        final Journal journal, final Executor executor) {
        final List<MigrationApi.Provider> copy = new ArrayList<MigrationApi.Provider>(
                Objects.requireNonNull(providers, "providers"));
        if (copy.isEmpty() || copy.contains(null)) {
            throw new IllegalArgumentException("providers must be non-empty");
        }
        Collections.sort(copy, Comparator.comparing(MigrationApi.Provider::id));
        final Set<String> ids = new HashSet<String>();
        for (MigrationApi.Provider provider : copy) {
            if (!ids.add(provider.id())) {
                throw new IllegalArgumentException("duplicate provider " + provider.id());
            }
        }
        this.providers = Collections.unmodifiableList(copy);
        this.target = Objects.requireNonNull(target, "target");
        this.backups = Objects.requireNonNull(backups, "backups");
        this.journal = Objects.requireNonNull(journal, "journal");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    @Override public CompletionStage<MigrationApi.Plan> plan(final MigrationApi.Request request) {
        Objects.requireNonNull(request, "request");
        return CompletableFuture.supplyAsync(() -> createPlan(request), executor);
    }

    @Override public CompletionStage<MigrationApi.Report> execute(
            final MigrationApi.Request request) {
        Objects.requireNonNull(request, "request");
        return CompletableFuture.supplyAsync(() -> executeNow(request), executor);
    }

    @Override public CompletionStage<MigrationApi.Report> rollback(final String migrationId) {
        Objects.requireNonNull(migrationId, "migrationId");
        return CompletableFuture.supplyAsync(() -> rollbackNow(migrationId), executor);
    }

    private MigrationApi.Report executeNow(final MigrationApi.Request request) {
        final MigrationApi.Plan plan = createPlan(request);
        if (!plan.applicable()) {
            return report(request, MigrationApi.Status.REJECTED, plan);
        }
        if (request.mode() == MigrationApi.Mode.DRY_RUN) {
            return report(request, MigrationApi.Status.PLANNED, plan);
        }
        final List<MigrationApi.Record> snapshot = immutableSnapshot(target.snapshot());
        final String backupId = validatedBackupId(
                backups.capture(request.migrationId(), snapshot));
        journal.prepared(request.migrationId(), backupId, plan);
        try {
            target.apply(plan.targetRecords(), request.conflictPolicy());
            journal.applied(request.migrationId());
            return report(request, MigrationApi.Status.APPLIED, plan);
        } catch (RuntimeException failure) {
            backups.restore(backupId, target);
            journal.failed(request.migrationId(), failure.getClass().getSimpleName());
            final List<String> findings = new ArrayList<String>(plan.findings());
            findings.add("apply-failed-restored");
            return new MigrationApi.Report(request.migrationId(), MigrationApi.Status.FAILED,
                    request.records().size(), plan.targetRecords().size(), findings);
        }
    }

    private MigrationApi.Report rollbackNow(final String migrationId) {
        final Journal.Entry entry = journal.entry(migrationId);
        if (entry == null || !entry.applied()) {
            return new MigrationApi.Report(migrationId, MigrationApi.Status.REJECTED,
                    0, 0, Collections.singletonList("migration-not-applied"));
        }
        backups.restore(entry.backupId(), target);
        journal.rolledBack(migrationId);
        return new MigrationApi.Report(migrationId, MigrationApi.Status.ROLLED_BACK,
                0, target.snapshot().size(), Collections.singletonList("backup-restored"));
    }

    private MigrationApi.Plan createPlan(final MigrationApi.Request request) {
        final List<MigrationApi.Record> output = new ArrayList<MigrationApi.Record>();
        final List<String> findings = new ArrayList<String>();
        boolean applicable = true;
        for (MigrationApi.Record source : request.records()) {
            final MigrationApi.Provider provider = provider(source.kind());
            if (provider == null) {
                findings.add("unsupported:" + source.id());
                applicable = false;
                continue;
            }
            final MigrationApi.Conversion conversion = Objects.requireNonNull(
                    provider.convert(source), "conversion");
            findings.add(conversion.state().name().toLowerCase(java.util.Locale.ROOT)
                    + ":" + source.id() + ":" + conversion.reason());
            if (conversion.state() == MigrationApi.ConversionState.UNSUPPORTED) {
                applicable = false;
            } else {
                output.addAll(conversion.records());
            }
        }
        Collections.sort(output);
        final Set<String> seen = new HashSet<String>();
        final Set<String> existing = target.ids();
        final List<MigrationApi.Record> accepted = new ArrayList<MigrationApi.Record>();
        for (MigrationApi.Record record : output) {
            if (!seen.add(record.id())) {
                findings.add("duplicate-output:" + record.id());
                applicable = false;
            } else if (existing.contains(record.id())
                    && request.conflictPolicy() == MigrationApi.ConflictPolicy.FAIL) {
                findings.add("existing-target:" + record.id());
                applicable = false;
            } else if (existing.contains(record.id())
                    && request.conflictPolicy() == MigrationApi.ConflictPolicy.KEEP_EXISTING) {
                findings.add("kept-existing:" + record.id());
            } else {
                accepted.add(record);
            }
        }
        return new MigrationApi.Plan(request.migrationId(), request.mode(), accepted,
                findings, applicable);
    }

    private MigrationApi.Provider provider(final String kind) {
        MigrationApi.Provider match = null;
        for (MigrationApi.Provider candidate : providers) {
            if (candidate.supports(kind)) {
                if (match != null) {
                    throw new IllegalStateException("ambiguous provider for " + kind);
                }
                match = candidate;
            }
        }
        return match;
    }

    private static List<MigrationApi.Record> immutableSnapshot(
            final List<MigrationApi.Record> snapshot) {
        final List<MigrationApi.Record> copy = new ArrayList<MigrationApi.Record>(
                Objects.requireNonNull(snapshot, "snapshot"));
        if (copy.contains(null)) {
            throw new IllegalStateException("target snapshot contains null");
        }
        Collections.sort(copy);
        return Collections.unmodifiableList(copy);
    }

    private static String validatedBackupId(final String value) {
        if (value == null) {
            throw new IllegalStateException("backup store returned null identity");
        }
        final String checked = value.trim();
        if (!checked.matches("[a-z0-9][a-z0-9_.:/-]{0,127}")) {
            throw new IllegalStateException("backup store returned invalid identity");
        }
        return checked;
    }

    private static MigrationApi.Report report(final MigrationApi.Request request,
                                              final MigrationApi.Status status,
                                              final MigrationApi.Plan plan) {
        return new MigrationApi.Report(request.migrationId(), status, request.records().size(),
                plan.targetRecords().size(), plan.findings());
    }

    /** Atomic target boundary owned by the destination feature. */
    public interface Target {
        /** @return immutable current snapshot */
        List<MigrationApi.Record> snapshot();
        /** @return stable current record IDs */
        Set<String> ids();
        /** Applies all records atomically or throws without a partial durable result. */
        void apply(List<MigrationApi.Record> records, MigrationApi.ConflictPolicy policy);
        /** Replaces state atomically during rollback. */
        void restore(List<MigrationApi.Record> snapshot);
    }

    /** Backup boundary. */
    public interface BackupStore {
        /** Captures and validates a backup before apply. */
        String capture(String migrationId, List<MigrationApi.Record> snapshot);
        /** Restores one validated backup. */
        void restore(String backupId, Target target);
    }

    /** Append-only migration audit journal. */
    public interface Journal {
        /** Records the validated plan and backup before mutation. */
        void prepared(String migrationId, String backupId, MigrationApi.Plan plan);
        /** Records successful atomic application. */
        void applied(String migrationId);
        /** Records restored failure without sensitive exception text. */
        void failed(String migrationId, String failureType);
        /** Records rollback. */
        void rolledBack(String migrationId);
        /** @return prior entry, or null when absent */
        Entry entry(String migrationId);

        /** Immutable journal projection. */
        interface Entry {
            /** @return validated backup identity */ String backupId();
            /** @return whether apply completed */ boolean applied();
        }
    }
}
