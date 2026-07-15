package io.zartra.bedwars.storage.api;

import io.zartra.bedwars.api.result.Result;

/**
 * Thread-confined durable transaction boundary.
 *
 * <p>The creating thread owns the unit. {@link #close()} rolls back an active transaction and is
 * idempotent. A caller explicitly commits only after every mutation succeeds.</p>
 */
public interface UnitOfWork extends AutoCloseable {
    /** @return transaction state */ State state();
    /** @return typed commit result */ Result<State> commit();
    /** @return typed rollback result */ Result<State> rollback();
    /** Rolls back if active; never commits. */ @Override void close();

    /** Transaction lifecycle state. */
    enum State {
        /** Operations may be executed. */ ACTIVE,
        /** Transaction committed durably. */ COMMITTED,
        /** Transaction was rolled back. */ ROLLED_BACK,
        /** Resource was closed after completion. */ CLOSED
    }
}
