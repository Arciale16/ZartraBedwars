package io.zartra.bedwars.storage.sql;

import io.zartra.bedwars.storage.api.StorageEngine.EngineKind;

/** Enforces SQL authority constraints before any pool is opened. */
public final class SqlTopologyPolicy {
    private SqlTopologyPolicy() { }

    /**
     * Validates the requested deployment topology.
     *
     * @param kind selected SQL authority
     * @param scalableProxy whether multiple backend JVMs may mutate durable state
     * @throws IllegalArgumentException when SQLite is selected for scalable topology
     */
    public static void validate(final EngineKind kind, final boolean scalableProxy) {
        if (kind == null) { throw new NullPointerException("kind"); }
        if (scalableProxy && kind == EngineKind.SQLITE) {
            throw new IllegalArgumentException("scalable-proxy mode requires MySQL or MariaDB");
        }
    }
}
