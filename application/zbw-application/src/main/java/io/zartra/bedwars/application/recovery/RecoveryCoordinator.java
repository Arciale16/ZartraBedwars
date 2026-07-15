package io.zartra.bedwars.application.recovery;

import io.zartra.bedwars.api.failure.FailureKind;
import io.zartra.bedwars.api.failure.FailureReport;
import io.zartra.bedwars.api.failure.FailureSink;
import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.recovery.Recovery;
import io.zartra.bedwars.api.result.ApiError;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.api.scheduler.SchedulerPort;
import io.zartra.bedwars.api.scheduler.TaskDescriptor;
import io.zartra.bedwars.api.time.TimeSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Bounded idempotent recovery coordinator over an injected durable marker port. */
public final class RecoveryCoordinator {
    private static final DefinitionId INVALID_TRANSITION =
            DefinitionId.of("zartra", "recovery/invalid-transition");
    private static final DefinitionId INCOMPLETE =
            DefinitionId.of("zartra", "recovery/incomplete");
    private final List<Recovery.Step> steps;
    private final Recovery.MarkerStore markerStore;
    private final SchedulerPort scheduler;
    private final TimeSource timeSource;
    private final FailureSink failureSink;
    /** Creates a coordinator in exact transition order. */
    public RecoveryCoordinator(final List<Recovery.Step> steps,
                               final Recovery.MarkerStore markerStore,
                               final SchedulerPort scheduler,
                               final TimeSource timeSource,
                               final FailureSink failureSink) {
        final List<Recovery.Step> copy = new ArrayList<Recovery.Step>(
                Objects.requireNonNull(steps, "steps"));
        final Set<DefinitionId> ids = new HashSet<DefinitionId>();
        for (Recovery.Step step : copy) {
            Objects.requireNonNull(step, "step");
            if (!ids.add(Objects.requireNonNull(step.id(), "step.id"))) {
                throw new IllegalArgumentException("duplicate recovery step ID");
            }
        }
        this.steps = Collections.unmodifiableList(copy);
        this.markerStore = Objects.requireNonNull(markerStore, "markerStore");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.timeSource = Objects.requireNonNull(timeSource, "timeSource");
        this.failureSink = Objects.requireNonNull(failureSink, "failureSink");
    }

    /** Runs persistence and recovery steps only on the bounded worker. */
    public SchedulerPort.TaskHandle<Recovery.Report> recover(final TaskDescriptor descriptor,
                                                              final Recovery.Marker initial) {
        Objects.requireNonNull(descriptor, "descriptor");
        Objects.requireNonNull(initial, "initial");
        if (initial.state() != Recovery.State.DETECTED || initial.revision() != 0L) {
            throw new IllegalArgumentException("initial marker must be DETECTED at revision zero");
        }
        return scheduler.submit(descriptor, context -> execute(initial, descriptor.correlationId()));
    }

    private Recovery.Report execute(final Recovery.Marker initial,
                                    final CorrelationId correlationId) {
        final List<FailureReport> failures = new ArrayList<FailureReport>();
        Result<Recovery.Marker> persisted = markerStore.save(initial, -1L);
        if (persisted.isFailure()) {
            add(failures, failure(persisted.error().get(), correlationId));
            return new Recovery.Report(initial, failures);
        }
        Recovery.Marker current = persisted.requireValue();
        for (Recovery.Step step : steps) {
            final Result<Recovery.State> result = step.execute(current);
            if (result.isFailure()) {
                add(failures, failure(result.error().get(), correlationId));
                current = manual(current, failures, correlationId);
                return new Recovery.Report(current, failures);
            }
            final Recovery.State next = result.requireValue();
            if (next.ordinal() <= current.state().ordinal()
                    || next.ordinal() > Recovery.State.RECONCILED.ordinal()) {
                add(failures, FailureReport.of(INVALID_TRANSITION, FailureKind.INVALID,
                        correlationId, "recovery.invalid_transition", false, timeSource.now()));
                current = manual(current, failures, correlationId);
                return new Recovery.Report(current, failures);
            }
            persisted = markerStore.save(current.advance(next, timeSource.now()),
                    current.revision());
            if (persisted.isFailure()) {
                add(failures, failure(persisted.error().get(), correlationId));
                current = manual(current, failures, correlationId);
                return new Recovery.Report(current, failures);
            }
            current = persisted.requireValue();
        }
        if (current.state() != Recovery.State.RECONCILED) {
            add(failures, FailureReport.of(INCOMPLETE, FailureKind.INVALID, correlationId,
                    "recovery.incomplete", false, timeSource.now()));
            current = manual(current, failures, correlationId);
            return new Recovery.Report(current, failures);
        }
        persisted = markerStore.save(current.advance(Recovery.State.RECOVERED, timeSource.now()),
                current.revision());
        if (persisted.isFailure()) {
            add(failures, failure(persisted.error().get(), correlationId));
            current = manual(current, failures, correlationId);
        } else {
            current = persisted.requireValue();
        }
        return new Recovery.Report(current, failures);
    }

    private Recovery.Marker manual(final Recovery.Marker current,
                                   final List<FailureReport> failures,
                                   final CorrelationId correlationId) {
        final Result<Recovery.Marker> persisted = markerStore.save(
                current.advance(Recovery.State.MANUAL_REQUIRED, timeSource.now()),
                current.revision());
        if (persisted.isFailure()) {
            add(failures, failure(persisted.error().get(), correlationId));
            return current;
        }
        return persisted.requireValue();
    }

    private FailureReport failure(final ApiError error, final CorrelationId correlationId) {
        return FailureReport.of(error.code(), FailureKind.INTERNAL, correlationId,
                error.messageKey(), error.retryDisposition() == ApiError.RetryDisposition.RETRYABLE,
                timeSource.now());
    }

    private void add(final List<FailureReport> failures, final FailureReport failure) {
        failures.add(failure);
        try { failureSink.publish(failure); }
        catch (RuntimeException ignored) { return; }
    }
}
