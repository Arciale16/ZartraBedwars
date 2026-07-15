package io.zartra.bedwars.world.api;

import io.zartra.bedwars.api.identity.DefinitionId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Immutable terminal result of a bounded world operation. */
public final class WorldOperationResult {
    private final WorldOperation operation;
    private final Status status;
    private final DefinitionId reason;
    private final List<DefinitionId> completedSteps;
    private final boolean rollbackComplete;
    private final WorldProvider.ResourceSnapshot resources;

    /** Creates a validated terminal result. */
    public WorldOperationResult(final WorldOperation operation, final Status status,
                                final DefinitionId reason, final List<DefinitionId> completedSteps,
                                final boolean rollbackComplete,
                                final WorldProvider.ResourceSnapshot resources) {
        this.operation = Objects.requireNonNull(operation, "operation");
        this.status = Objects.requireNonNull(status, "status");
        this.reason = Objects.requireNonNull(reason, "reason");
        final List<DefinitionId> copy = new ArrayList<DefinitionId>(
                Objects.requireNonNull(completedSteps, "completedSteps"));
        if (copy.contains(null)) { throw new IllegalArgumentException("completedSteps contains null"); }
        this.completedSteps = Collections.unmodifiableList(copy);
        this.rollbackComplete = rollbackComplete;
        this.resources = Objects.requireNonNull(resources, "resources");
    }
    /** @return request */ public WorldOperation operation() { return operation; }
    /** @return terminal classification */ public Status status() { return status; }
    /** @return stable reason */ public DefinitionId reason() { return reason; }
    /** @return successfully executed step IDs */ public List<DefinitionId> completedSteps() { return completedSteps; }
    /** @return whether every required compensation succeeded */ public boolean rollbackComplete() { return rollbackComplete; }
    /** @return terminal leak/accounting snapshot */ public WorldProvider.ResourceSnapshot resources() { return resources; }

    /** Terminal world-operation state. */ public enum Status { SUCCEEDED, FAILED, CANCELLED, TIMED_OUT, REJECTED }
}
