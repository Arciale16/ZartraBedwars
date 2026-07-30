package io.zartra.bedwars.config.m23;

import io.zartra.bedwars.api.migration.MigrationApi;
import java.util.Objects;

/** Immutable validated policy for the M23 layout migration workflow. */
public final class M23MigrationConfiguration {
    private final boolean enabled;
    private final int maximumSourceBytes;
    private final int maximumRecords;
    private final MigrationApi.ConflictPolicy defaultConflictPolicy;
    private final boolean backupRequired;

    /** Creates a validated migration policy. */
    public M23MigrationConfiguration(final boolean enabled, final int maximumSourceBytes,
                                     final int maximumRecords,
                                     final MigrationApi.ConflictPolicy defaultConflictPolicy,
                                     final boolean backupRequired) {
        if (maximumSourceBytes < 1024 || maximumSourceBytes > 1048576) {
            throw new IllegalArgumentException("maximumSourceBytes outside 1024..1048576");
        }
        if (maximumRecords < 1 || maximumRecords > 10000) {
            throw new IllegalArgumentException("maximumRecords outside 1..10000");
        }
        if (!backupRequired) {
            throw new IllegalArgumentException("M23 migrations require backup");
        }
        this.enabled = enabled;
        this.maximumSourceBytes = maximumSourceBytes;
        this.maximumRecords = maximumRecords;
        this.defaultConflictPolicy =
                Objects.requireNonNull(defaultConflictPolicy, "defaultConflictPolicy");
        this.backupRequired = true;
    }

    /** @return safe disabled-by-default policy */
    public static M23MigrationConfiguration defaults() {
        return new M23MigrationConfiguration(false, 1048576, 10000,
                MigrationApi.ConflictPolicy.FAIL, true);
    }
    /** @return whether operator explicitly enabled migration */ public boolean enabled() {
        return enabled;
    }
    /** @return maximum source bytes */ public int maximumSourceBytes() {
        return maximumSourceBytes;
    }
    /** @return maximum source records */ public int maximumRecords() { return maximumRecords; }
    /** @return default fail-closed conflict policy */
    public MigrationApi.ConflictPolicy defaultConflictPolicy() { return defaultConflictPolicy; }
    /** @return mandatory backup flag */ public boolean backupRequired() {
        return backupRequired;
    }
}
