package io.zartra.bedwars.config.migration;

import io.zartra.bedwars.api.configuration.ConfigurationVersion;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.config.schema.ConfigurationModel.Document;
import io.zartra.bedwars.config.schema.ConfigurationModel.LogicalFile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Backup-first migration coordinator whose failure path always retains the original document. */
public final class ConfigurationMigrationService {
    private final LogicalFile file;
    private final Map<ConfigurationVersion, Step> bySource;

    /**
     * Creates a deterministic consecutive migration plan.
     *
     * @param file logical file governed by the plan
     * @param steps unique one-version steps
     */
    public ConfigurationMigrationService(final LogicalFile file, final Collection<Step> steps) {
        this.file = Objects.requireNonNull(file, "file");
        final Map<ConfigurationVersion, Step> collected =
                new LinkedHashMap<ConfigurationVersion, Step>();
        for (Step step : Objects.requireNonNull(steps, "steps")) {
            final Step checked = Objects.requireNonNull(step, "step");
            if (checked.to().value() != checked.from().value() + 1) {
                throw new IllegalArgumentException("Migration steps must advance exactly one version");
            }
            if (collected.put(checked.from(), checked) != null) {
                throw new IllegalArgumentException("Duplicate migration source version");
            }
        }
        bySource = Collections.unmodifiableMap(collected);
    }

    /**
     * Migrates to the requested version after a successful backup capture.
     *
     * @return success or a secret-free failure report retaining the original document
     */
    public MigrationResult migrate(final Document source, final ConfigurationVersion target,
                                   final BackupPort backupPort) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(backupPort, "backupPort");
        if (source.version().compareTo(target) > 0) {
            return MigrationResult.failure(source, DefinitionId.of("zartra", "migration/downgrade_forbidden"));
        }
        if (source.version().equals(target)) {
            return MigrationResult.success(source, source, Collections.<ConfigurationVersion>emptyList(), null);
        }
        final BackupReceipt receipt;
        try {
            receipt = Objects.requireNonNull(backupPort.capture(file, source), "backupReceipt");
        } catch (RuntimeException exception) {
            return MigrationResult.failure(source, DefinitionId.of("zartra", "migration/backup_failed"));
        }
        Document current = source;
        final List<ConfigurationVersion> applied = new ArrayList<ConfigurationVersion>();
        while (current.version().compareTo(target) < 0) {
            final Step step = bySource.get(current.version());
            if (step == null || step.to().compareTo(target) > 0) {
                return MigrationResult.failure(source, DefinitionId.of("zartra", "migration/step_missing"));
            }
            try {
                final Document migrated = Objects.requireNonNull(step.migrate(current), "migratedDocument");
                if (!migrated.version().equals(step.to())) {
                    return MigrationResult.failure(source,
                            DefinitionId.of("zartra", "migration/version_contract"));
                }
                current = migrated;
                applied.add(step.to());
            } catch (RuntimeException exception) {
                return MigrationResult.failure(source, DefinitionId.of("zartra", "migration/step_failed"));
            }
        }
        return MigrationResult.success(source, current, applied, receipt);
    }

    /** Pure one-version document transformation. */
    public interface Step {
        /** @return source version */ ConfigurationVersion from();
        /** @return target version, exactly one greater than source */ ConfigurationVersion to();
        /** @return new immutable migrated document */ Document migrate(Document source);
    }

    /** Port for durable backup capture before migration; implementations own I/O. */
    public interface BackupPort {
        /** @return non-secret receipt proving backup capture */
        BackupReceipt capture(LogicalFile file, Document source);
    }

    /** Immutable backup receipt containing identity only, never a path or secret. */
    public static final class BackupReceipt {
        private final DefinitionId id;
        private BackupReceipt(final DefinitionId id) { this.id = Objects.requireNonNull(id, "id"); }
        /** @return receipt for a captured backup */ public static BackupReceipt of(final DefinitionId id) {
            return new BackupReceipt(id);
        }
        /** @return stable backup identity */ public DefinitionId id() { return id; }
    }

    /** Immutable migration result. */
    public static final class MigrationResult {
        private final Document original;
        private final Document result;
        private final List<ConfigurationVersion> applied;
        private final BackupReceipt receipt;
        private final DefinitionId failure;
        private MigrationResult(final Document original, final Document result,
                                final Collection<ConfigurationVersion> applied,
                                final BackupReceipt receipt, final DefinitionId failure) {
            this.original = original;
            this.result = result;
            this.applied = Collections.unmodifiableList(new ArrayList<ConfigurationVersion>(applied));
            this.receipt = receipt;
            this.failure = failure;
        }
        private static MigrationResult success(final Document original, final Document result,
                                               final Collection<ConfigurationVersion> applied,
                                               final BackupReceipt receipt) {
            return new MigrationResult(original, result, applied, receipt, null);
        }
        private static MigrationResult failure(final Document original, final DefinitionId failure) {
            return new MigrationResult(original, original, Collections.<ConfigurationVersion>emptyList(),
                    null, failure);
        }
        /** @return whether every requested step completed */ public boolean isSuccess() { return failure == null; }
        /** @return unchanged source document */ public Document original() { return original; }
        /** @return migrated document, or original on failure */ public Document result() { return result; }
        /** @return target versions applied in order */ public List<ConfigurationVersion> appliedVersions() { return applied; }
        /** @return backup receipt when capture was required and successful */ public Optional<BackupReceipt> backup() { return Optional.ofNullable(receipt); }
        /** @return stable failure code */ public Optional<DefinitionId> failure() { return Optional.ofNullable(failure); }
    }
}
