package io.zartra.bedwars.application.scheduler;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.scheduler.SchedulerPort;
import java.util.Objects;

/** Fail-fast thread guard backed by an injected platform ownership query. */
public final class StrictThreadGuard implements SchedulerPort.ThreadGuard {
    private final SchedulerPort.OwnerThreadDispatcher dispatcher;
    /** @param dispatcher platform ownership query */
    public StrictThreadGuard(final SchedulerPort.OwnerThreadDispatcher dispatcher) {
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
    }
    @Override public void requireOwnerThread(final DefinitionId ownerId) {
        Objects.requireNonNull(ownerId, "ownerId");
        if (!dispatcher.isOwnerThread(ownerId)) {
            throw new SchedulerPort.ThreadAccessException(ownerId,
                    "Operation requires its owning execution context");
        }
    }
    @Override public void requireWorkerThread(final DefinitionId ownerId) {
        Objects.requireNonNull(ownerId, "ownerId");
        if (dispatcher.isOwnerThread(ownerId)) {
            throw new SchedulerPort.ThreadAccessException(ownerId,
                    "Blocking operation is forbidden on its owning execution context");
        }
    }
}
