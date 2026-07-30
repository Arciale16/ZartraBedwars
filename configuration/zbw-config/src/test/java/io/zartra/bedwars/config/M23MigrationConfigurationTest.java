package io.zartra.bedwars.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.migration.MigrationApi;
import io.zartra.bedwars.config.m23.M23MigrationConfiguration;
import org.junit.jupiter.api.Test;

class M23MigrationConfigurationTest {
    @Test
    void defaultsAreDisabledBoundedAndFailClosed() {
        final M23MigrationConfiguration policy = M23MigrationConfiguration.defaults();
        assertFalse(policy.enabled());
        assertTrue(policy.backupRequired());
        assertEquals(1048576, policy.maximumSourceBytes());
        assertEquals(10000, policy.maximumRecords());
        assertEquals(MigrationApi.ConflictPolicy.FAIL, policy.defaultConflictPolicy());
    }

    @Test
    void rejectsUnsafeLimitsAndBackupDisablement() {
        assertThrows(IllegalArgumentException.class, () -> new M23MigrationConfiguration(
                true, 1, 1, MigrationApi.ConflictPolicy.FAIL, true));
        assertThrows(IllegalArgumentException.class, () -> new M23MigrationConfiguration(
                true, 1024, 10001, MigrationApi.ConflictPolicy.FAIL, true));
        assertThrows(IllegalArgumentException.class, () -> new M23MigrationConfiguration(
                true, 1024, 1, MigrationApi.ConflictPolicy.FAIL, false));
    }
}
