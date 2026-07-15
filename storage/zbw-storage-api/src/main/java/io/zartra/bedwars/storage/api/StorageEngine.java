package io.zartra.bedwars.storage.api;

import io.zartra.bedwars.api.result.Result;

/** Lifecycle and transaction entry point for one authoritative durable store. */
public interface StorageEngine extends AutoCloseable {
    /** @return backend engine kind */ EngineKind kind();
    /** @return a thread-confined active transaction */ Result<UnitOfWork> begin(TransactionOptions options);
    /** @return repository bound to this engine */ StorageRepository records();
    /** @return durable message repository bound to this engine */ MessageRepository messages();
    /** @return privacy retention repository bound to this engine */ RetentionRepository retention();
    /** Releases pools and rejects subsequent transactions. */ @Override void close();

    /** Supported SQL authority engines. */
    enum EngineKind {
        /** One-JVM serialized-writer embedded deployment. */ SQLITE,
        /** External MySQL authority. */ MYSQL,
        /** External MariaDB authority. */ MARIADB
    }
}
