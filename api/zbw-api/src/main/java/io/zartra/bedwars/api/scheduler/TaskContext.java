package io.zartra.bedwars.api.scheduler;

import java.util.Objects;

/** Immutable context passed to accepted work. */
public final class TaskContext {
    private final TaskDescriptor descriptor;
    private final CancellationToken cancellationToken;

    /** @param descriptor task metadata @param cancellationToken cooperative token */
    public TaskContext(final TaskDescriptor descriptor, final CancellationToken cancellationToken) {
        this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
        this.cancellationToken = Objects.requireNonNull(cancellationToken, "cancellationToken");
    }
    /** @return immutable admission metadata */ public TaskDescriptor descriptor() { return descriptor; }
    /** @return cooperative cancellation/deadline signal */ public CancellationToken cancellationToken() { return cancellationToken; }
}
