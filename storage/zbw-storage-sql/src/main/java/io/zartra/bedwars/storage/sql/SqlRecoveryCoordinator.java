package io.zartra.bedwars.storage.sql;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.result.ApiError;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.storage.api.RecoveryService;
import java.time.Instant;
import java.util.Objects;

/** Validates backup/restore evidence around a database-specific encrypted backup driver. */
public final class SqlRecoveryCoordinator implements RecoveryService {
    private static final ApiError INVALID_BACKUP = ApiError.of(
            DefinitionId.of("zartra", "storage.invalid_backup"), "storage.error.invalid_backup",
            ApiError.RetryDisposition.PERMANENT);
    private final BackupDriver driver;
    private final RecoveryObjectives objectives;

    /** Creates an operational coordinator for an injected database backup driver. */
    public SqlRecoveryCoordinator(final BackupDriver driver, final RecoveryObjectives objectives) {
        this.driver = Objects.requireNonNull(driver, "driver");
        this.objectives = Objects.requireNonNull(objectives, "objectives");
    }

    @Override public Result<BackupEvidence> backup(final DefinitionId backupId,
                                                   final Instant requestedAt) {
        if (backupId == null || requestedAt == null) { throw new NullPointerException("backup argument"); }
        final Result<BackupArtifact> result = driver.createEncrypted(backupId, requestedAt);
        return evidence(backupId, result);
    }

    @Override public Result<BackupEvidence> restore(final DefinitionId backupId,
                                                    final Instant requestedAt) {
        if (backupId == null || requestedAt == null) { throw new NullPointerException("restore argument"); }
        final Result<BackupArtifact> validation = driver.validate(backupId);
        if (validation.isFailure()) { return Result.failure(validation.error().get()); }
        if (!valid(validation.value().get())) { return Result.failure(INVALID_BACKUP); }
        return evidence(backupId, driver.restoreQuiescent(backupId, requestedAt));
    }

    @Override public RecoveryObjectives objectives() { return objectives; }

    private static Result<BackupEvidence> evidence(final DefinitionId backupId,
                                                   final Result<BackupArtifact> result) {
        if (result.isFailure()) { return Result.failure(result.error().get()); }
        final BackupArtifact artifact = result.value().get();
        if (!valid(artifact)) { return Result.failure(INVALID_BACKUP); }
        return Result.success(BackupEvidence.of(backupId, artifact.completedAt(), artifact.checksum()));
    }

    private static boolean valid(final BackupArtifact artifact) {
        return artifact != null && artifact.encrypted() && artifact.validated()
                && artifact.checksum().matches("[0-9a-f]{64}");
    }

    /** Database-specific provider; implementations must quiesce writes before restore. */
    public interface BackupDriver {
        /** @return encrypted and independently validated artifact evidence */
        Result<BackupArtifact> createEncrypted(DefinitionId backupId, Instant requestedAt);
        /** @return validation evidence without mutating the database */
        Result<BackupArtifact> validate(DefinitionId backupId);
        /** @return evidence after a quiescent restore and integrity check */
        Result<BackupArtifact> restoreQuiescent(DefinitionId backupId, Instant requestedAt);
    }

    /** Immutable, path-free and secret-free backup-provider evidence. */
    public static final class BackupArtifact {
        private final Instant completedAt;
        private final String checksum;
        private final boolean encrypted;
        private final boolean validated;

        private BackupArtifact(final Instant completedAt, final String checksum,
                               final boolean encrypted, final boolean validated) {
            this.completedAt = Objects.requireNonNull(completedAt, "completedAt");
            if (checksum == null) { throw new NullPointerException("checksum"); }
            this.checksum = checksum;
            this.encrypted = encrypted;
            this.validated = validated;
        }
        /** @return provider evidence */
        public static BackupArtifact of(final Instant completedAt, final String checksum,
                                        final boolean encrypted, final boolean validated) {
            return new BackupArtifact(completedAt, checksum, encrypted, validated);
        }
        /** @return completion instant */ public Instant completedAt() { return completedAt; }
        /** @return SHA-256 artifact checksum */ public String checksum() { return checksum; }
        /** @return whether encryption was verified */ public boolean encrypted() { return encrypted; }
        /** @return whether restore/read integrity was verified */ public boolean validated() { return validated; }
    }
}
