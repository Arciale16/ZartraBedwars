package io.zartra.bedwars.application.lifecycle;

import io.zartra.bedwars.api.failure.FailureKind;
import io.zartra.bedwars.api.failure.FailureReport;
import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.lifecycle.Lifecycle;
import io.zartra.bedwars.api.result.ApiError;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.api.scheduler.SchedulerPort;
import io.zartra.bedwars.api.scheduler.TaskDescriptor;
import io.zartra.bedwars.api.time.MonotonicTimeSource;
import io.zartra.bedwars.api.time.TimeSource;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/** Ordered lifecycle coordinator executed exclusively through a bounded scheduler. */
public final class LifecycleCoordinator {
    private static final DefinitionId DEADLINE = DefinitionId.of("zartra", "lifecycle/deadline");
    private final List<Lifecycle.Component> components;
    private final SchedulerPort scheduler;
    private final MonotonicTimeSource monotonic;
    private final TimeSource wallClock;
    private final AtomicReference<Lifecycle.State> state =
            new AtomicReference<Lifecycle.State>(Lifecycle.State.NEW);
    /** Creates a coordinator in declared dependency order. */
    public LifecycleCoordinator(final List<Lifecycle.Component> components,
                                final SchedulerPort scheduler,
                                final MonotonicTimeSource monotonic,
                                final TimeSource wallClock) {
        final List<Lifecycle.Component> copy = new ArrayList<Lifecycle.Component>(
                Objects.requireNonNull(components, "components"));
        final Set<DefinitionId> ids = new HashSet<DefinitionId>();
        for (Lifecycle.Component component : copy) {
            Objects.requireNonNull(component, "component");
            if (!ids.add(Objects.requireNonNull(component.id(), "component.id"))) {
                throw new IllegalArgumentException("duplicate lifecycle component ID");
            }
        }
        this.components = Collections.unmodifiableList(copy);
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.monotonic = Objects.requireNonNull(monotonic, "monotonic");
        this.wallClock = Objects.requireNonNull(wallClock, "wallClock");
    }
    /** Starts components in dependency order on the lifecycle worker. */
    public SchedulerPort.TaskHandle<Lifecycle.Report> start(final TaskDescriptor descriptor) {
        if (!state.compareAndSet(Lifecycle.State.NEW, Lifecycle.State.RUNNING)) {
            throw new IllegalStateException("lifecycle can be started exactly once");
        }
        final SchedulerPort.TaskHandle<Lifecycle.Report> handle = scheduler.submit(descriptor,
                context -> startComponents(deadline(context.descriptor().timeout()),
                        descriptor.correlationId()));
        handle.completion().thenAccept(outcome -> {
            if (!outcome.isSuccess()) { state.set(Lifecycle.State.FAILED); }
        });
        return handle;
    }
    /** Drains and stops components in reverse order on the lifecycle worker. */
    public SchedulerPort.TaskHandle<Lifecycle.Report> shutdown(final TaskDescriptor descriptor) {
        if (!state.compareAndSet(Lifecycle.State.RUNNING, Lifecycle.State.DRAINING)) {
            throw new IllegalStateException("lifecycle is not running");
        }
        final SchedulerPort.TaskHandle<Lifecycle.Report> handle = scheduler.submit(descriptor,
                context -> stopComponents(deadline(context.descriptor().timeout()),
                        descriptor.correlationId()));
        handle.completion().thenAccept(outcome -> {
            if (!outcome.isSuccess()) { state.set(Lifecycle.State.FAILED); }
        });
        return handle;
    }
    /** @return current state */ public Lifecycle.State state() { return state.get(); }

    private Lifecycle.Report startComponents(final long deadlineNanos,
                                             final CorrelationId correlationId) {
        final List<DefinitionId> started = new ArrayList<DefinitionId>();
        final List<FailureReport> failures = new ArrayList<FailureReport>();
        for (Lifecycle.Component component : components) {
            if (expired(deadlineNanos)) {
                failures.add(deadlineFailure(correlationId));
                rollback(started, deadlineNanos, failures, correlationId);
                state.set(Lifecycle.State.FAILED);
                return new Lifecycle.Report(Lifecycle.State.FAILED, started, failures, true);
            }
            final Result<Lifecycle.State> result = component.start(remaining(deadlineNanos));
            if (result.isFailure() || result.requireValue() != Lifecycle.State.RUNNING) {
                failures.add(result.isFailure() ? failure(result.error().get(), correlationId)
                        : invalidState(component.id(), correlationId));
                rollback(started, deadlineNanos, failures, correlationId);
                state.set(Lifecycle.State.FAILED);
                return new Lifecycle.Report(Lifecycle.State.FAILED, started, failures, true);
            }
            started.add(component.id());
        }
        return new Lifecycle.Report(Lifecycle.State.RUNNING, started, failures, false);
    }

    private Lifecycle.Report stopComponents(final long deadlineNanos,
                                            final CorrelationId correlationId) {
        final List<DefinitionId> stopped = new ArrayList<DefinitionId>();
        final List<FailureReport> failures = new ArrayList<FailureReport>();
        boolean forced = false;
        for (int index = components.size() - 1; index >= 0; index--) {
            final Lifecycle.Component component = components.get(index);
            if (expired(deadlineNanos)) {
                failures.add(deadlineFailure(correlationId));
                forced |= force(component, failures, correlationId);
                continue;
            }
            final Result<Lifecycle.State> drained = component.drain(remaining(deadlineNanos));
            final Result<Lifecycle.State> stoppedResult = drained.isSuccess()
                    ? component.stop(remaining(deadlineNanos)) : Result.failure(drained.error().get());
            if (stoppedResult.isSuccess() && stoppedResult.requireValue() == Lifecycle.State.STOPPED) {
                stopped.add(component.id());
            } else {
                failures.add(stoppedResult.isFailure()
                        ? failure(stoppedResult.error().get(), correlationId)
                        : invalidState(component.id(), correlationId));
                forced |= force(component, failures, correlationId);
            }
        }
        final Lifecycle.State terminal = failures.isEmpty()
                ? Lifecycle.State.STOPPED : Lifecycle.State.FORCED;
        state.set(terminal);
        return new Lifecycle.Report(terminal, stopped, failures, forced);
    }

    private void rollback(final List<DefinitionId> started, final long deadlineNanos,
                          final List<FailureReport> failures, final CorrelationId correlationId) {
        for (int index = started.size() - 1; index >= 0; index--) {
            final Lifecycle.Component component = find(started.get(index));
            if (expired(deadlineNanos)) {
                force(component, failures, correlationId);
            } else {
                final Result<Lifecycle.State> drained = component.drain(remaining(deadlineNanos));
                final Result<Lifecycle.State> stopped = drained.isSuccess()
                        ? component.stop(remaining(deadlineNanos)) : Result.failure(drained.error().get());
                if (stopped.isFailure()) {
                    failures.add(failure(stopped.error().get(), correlationId));
                    force(component, failures, correlationId);
                }
            }
        }
    }

    private boolean force(final Lifecycle.Component component,
                          final List<FailureReport> failures,
                          final CorrelationId correlationId) {
        final Result<Lifecycle.State> result = component.forceStop();
        if (result.isFailure()) {
            failures.add(failure(result.error().get(), correlationId));
        } else if (result.requireValue() != Lifecycle.State.FORCED
                && result.requireValue() != Lifecycle.State.STOPPED) {
            failures.add(invalidState(component.id(), correlationId));
        }
        return true;
    }

    private Lifecycle.Component find(final DefinitionId id) {
        for (Lifecycle.Component component : components) {
            if (component.id().equals(id)) { return component; }
        }
        throw new IllegalStateException("started component is absent");
    }

    private long deadline(final Duration timeout) {
        final long now = monotonic.readNanos();
        final long delta;
        try { delta = timeout.toNanos(); }
        catch (ArithmeticException exception) { return Long.MAX_VALUE; }
        return delta > Long.MAX_VALUE - now ? Long.MAX_VALUE : now + delta;
    }
    private boolean expired(final long deadlineNanos) { return monotonic.readNanos() >= deadlineNanos; }
    private Duration remaining(final long deadlineNanos) {
        return Duration.ofNanos(Math.max(1L, deadlineNanos - monotonic.readNanos()));
    }
    private FailureReport deadlineFailure(final CorrelationId correlationId) {
        return FailureReport.of(DEADLINE, FailureKind.TIMEOUT, correlationId,
                "lifecycle.deadline", true, wallClock.now());
    }
    private FailureReport invalidState(final DefinitionId componentId,
                                       final CorrelationId correlationId) {
        return FailureReport.of(componentId, FailureKind.INTERNAL, correlationId,
                "lifecycle.invalid_state", false, wallClock.now());
    }
    private FailureReport failure(final ApiError error, final CorrelationId correlationId) {
        return FailureReport.of(error.code(), FailureKind.INTERNAL, correlationId,
                error.messageKey(), error.retryDisposition() == ApiError.RetryDisposition.RETRYABLE,
                wallClock.now());
    }
}
