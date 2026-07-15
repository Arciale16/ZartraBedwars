package io.zartra.bedwars.api.scheduler;

import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.TaskId;
import java.time.Duration;
import java.util.Objects;

/** Immutable admission metadata for one bounded task. */
public final class TaskDescriptor {
    private final TaskId taskId;
    private final DefinitionId operationId;
    private final DefinitionId ownerId;
    private final CorrelationId correlationId;
    private final Duration timeout;
    private final boolean idempotent;

    private TaskDescriptor(final TaskId taskId, final DefinitionId operationId,
                           final DefinitionId ownerId, final CorrelationId correlationId,
                           final Duration timeout, final boolean idempotent) {
        this.taskId = Objects.requireNonNull(taskId, "taskId");
        this.operationId = Objects.requireNonNull(operationId, "operationId");
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId");
        this.correlationId = Objects.requireNonNull(correlationId, "correlationId");
        this.timeout = Objects.requireNonNull(timeout, "timeout");
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        this.idempotent = idempotent;
    }

    /** @return validated descriptor */
    public static TaskDescriptor of(final TaskId taskId, final DefinitionId operationId,
                                    final DefinitionId ownerId, final CorrelationId correlationId,
                                    final Duration timeout, final boolean idempotent) {
        return new TaskDescriptor(taskId, operationId, ownerId, correlationId, timeout, idempotent);
    }
    /** @return unique task identity */ public TaskId taskId() { return taskId; }
    /** @return stable operation identity */ public DefinitionId operationId() { return operationId; }
    /** @return component that owns cancellation and shutdown */ public DefinitionId ownerId() { return ownerId; }
    /** @return correlation identity propagated to failures and diagnostics */ public CorrelationId correlationId() { return correlationId; }
    /** @return maximum queue plus execution duration */ public Duration timeout() { return timeout; }
    /** @return whether bounded retry may safely repeat the operation */ public boolean idempotent() { return idempotent; }
}
