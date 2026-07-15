package io.zartra.bedwars.storage.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Immutable, strictly ordered migration plan. */
public final class MigrationPlan {
    private final List<Migration> migrations;

    private MigrationPlan(final List<Migration> migrations) {
        if (migrations == null || migrations.isEmpty()) {
            throw new IllegalArgumentException("migrations must not be empty");
        }
        final List<Migration> copy = new ArrayList<Migration>(migrations);
        int expected = 1;
        for (Migration migration : copy) {
            Objects.requireNonNull(migration, "migration");
            if (migration.version() != expected) {
                throw new IllegalArgumentException("migrations must be contiguous and ordered from version 1");
            }
            expected++;
        }
        this.migrations = Collections.unmodifiableList(copy);
    }

    /** @return a validated ordered plan */
    public static MigrationPlan of(final List<Migration> migrations) { return new MigrationPlan(migrations); }
    /** @return immutable ordered migrations */ public List<Migration> migrations() { return migrations; }
    /** @return final schema version */ public int targetVersion() { return migrations.size(); }
    /** @return whether any step requires restore-based rollback */
    public boolean requiresBackup() {
        for (Migration migration : migrations) {
            if (migration.unsafeDdl()) { return true; }
        }
        return false;
    }
}
