package io.zartra.bedwars.storage.api;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.result.Result;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** Backup, validation and restore boundary for operator-controlled recovery workflows. */
public interface RecoveryService {
    /** Creates and validates an encrypted backup at an operator-provided secure destination. */
    Result<BackupEvidence> backup(DefinitionId backupId, Instant requestedAt);
    /** Restores only a previously validated backup while the storage service is quiescent. */
    Result<BackupEvidence> restore(DefinitionId backupId, Instant requestedAt);
    /** @return declared business-state recovery objectives */ RecoveryObjectives objectives();

    /** Immutable evidence emitted after backup or restore verification. */
    final class BackupEvidence {
        private final DefinitionId backupId;
        private final Instant completedAt;
        private final String checksum;

        private BackupEvidence(final DefinitionId backupId, final Instant completedAt,
                               final String checksum) {
            if (checksum == null || !checksum.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("checksum must be lowercase SHA-256");
            }
            this.backupId = Objects.requireNonNull(backupId, "backupId");
            this.completedAt = Objects.requireNonNull(completedAt, "completedAt");
            this.checksum = checksum;
        }
        /** @return validated evidence */
        public static BackupEvidence of(final DefinitionId backupId, final Instant completedAt,
                                        final String checksum) {
            return new BackupEvidence(backupId, completedAt, checksum);
        }
        /** @return backup identity */ public DefinitionId backupId() { return backupId; }
        /** @return verification completion instant */ public Instant completedAt() { return completedAt; }
        /** @return verified content checksum */ public String checksum() { return checksum; }
    }

    /** Immutable recovery-point and recovery-time objectives. */
    final class RecoveryObjectives {
        private final Duration maximumDataLoss;
        private final Duration maximumServiceRestore;

        private RecoveryObjectives(final Duration maximumDataLoss,
                                   final Duration maximumServiceRestore) {
            if (maximumDataLoss == null || maximumDataLoss.isNegative()) {
                throw new IllegalArgumentException("maximumDataLoss must be non-negative");
            }
            if (maximumServiceRestore == null || maximumServiceRestore.isZero()
                    || maximumServiceRestore.isNegative()) {
                throw new IllegalArgumentException("maximumServiceRestore must be positive");
            }
            this.maximumDataLoss = maximumDataLoss;
            this.maximumServiceRestore = maximumServiceRestore;
        }
        /** @return validated objectives */
        public static RecoveryObjectives of(final Duration maximumDataLoss,
                                            final Duration maximumServiceRestore) {
            return new RecoveryObjectives(maximumDataLoss, maximumServiceRestore);
        }
        /** @return maximum committed-state loss */ public Duration maximumDataLoss() { return maximumDataLoss; }
        /** @return maximum service restoration duration */ public Duration maximumServiceRestore() {
            return maximumServiceRestore;
        }
    }
}
