package io.zartra.bedwars.world.orchestration;

import io.zartra.bedwars.api.failure.FailureReport;
import io.zartra.bedwars.api.health.Health;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.lifecycle.Lifecycle;
import io.zartra.bedwars.api.result.ApiError;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.api.scheduler.CancellationToken;
import io.zartra.bedwars.api.scheduler.SchedulerPort;
import io.zartra.bedwars.api.scheduler.TaskDescriptor;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.world.api.WorldOperation;
import io.zartra.bedwars.world.api.WorldOperationResult;
import io.zartra.bedwars.world.api.WorldProvider;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/** Bounded, cancellable and rollback-capable world lifecycle coordinator. */
public final class WorldOrchestrator implements Lifecycle.Component, Health.Source {
    private static final DefinitionId COMPONENT = DefinitionId.of("zartra", "world/orchestrator");
    private static final DefinitionId REJECTED = DefinitionId.of("zartra", "world/rejected");
    private static final DefinitionId SUCCEEDED = DefinitionId.of("zartra", "world/succeeded");
    private static final DefinitionId CANCELLED = DefinitionId.of("zartra", "world/cancelled");
    private static final DefinitionId INTERNAL = DefinitionId.of("zartra", "world/internal_failure");
    private static final DefinitionId SCHEDULER_TIMEOUT = DefinitionId.of("zartra", "scheduler/timeout");
    private final SchedulerPort scheduler;
    private final SchedulerPort.OwnerThreadDispatcher ownerDispatcher;
    private final WorldProvider provider;
    private final TimeSource timeSource;
    private final int maximumInFlight;
    private final Map<String, Execution> active = new LinkedHashMap<String, Execution>();
    private final Map<io.zartra.bedwars.world.api.WorldKey, String> activeWorlds =
            new LinkedHashMap<io.zartra.bedwars.world.api.WorldKey, String>();
    private final AtomicBoolean accepting = new AtomicBoolean();
    private final AtomicLong accepted = new AtomicLong();
    private final AtomicLong completed = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();
    private final AtomicLong cancelled = new AtomicLong();
    private final AtomicLong rejected = new AtomicLong();

    /** Creates a stopped coordinator with an exact in-flight bound. */
    public WorldOrchestrator(final SchedulerPort scheduler,
                             final SchedulerPort.OwnerThreadDispatcher ownerDispatcher,
                             final WorldProvider provider, final TimeSource timeSource,
                             final int maximumInFlight) {
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.ownerDispatcher = Objects.requireNonNull(ownerDispatcher, "ownerDispatcher");
        this.provider = Objects.requireNonNull(provider, "provider");
        this.timeSource = Objects.requireNonNull(timeSource, "timeSource");
        if (maximumInFlight < 1 || maximumInFlight > 64) {
            throw new IllegalArgumentException("maximumInFlight must be between 1 and 64");
        }
        this.maximumInFlight = maximumInFlight;
    }

    /** @return admitted or typed-rejected operation handle */
    public OperationHandle submit(final WorldOperation operation) {
        Objects.requireNonNull(operation, "operation");
        synchronized (active) {
            if (!accepting.get() || active.size() >= maximumInFlight
                    || activeWorlds.containsKey(operation.target())) {
                rejected.incrementAndGet();
                return rejected(operation);
            }
            final WorldProvider.Plan plan;
            try { plan = provider.plan(operation); }
            catch (RuntimeException failure) {
                rejected.incrementAndGet();
                return rejected(operation);
            }
            final Execution execution = new Execution(plan);
            active.put(operation.operationId().toString(), execution);
            activeWorlds.put(operation.target(), operation.operationId().toString());
            accepted.incrementAndGet();
            execution.runNext();
            return execution;
        }
    }

    private OperationHandle rejected(final WorldOperation operation) {
        final CompletableFuture<WorldOperationResult> result = new CompletableFuture<WorldOperationResult>();
        result.complete(new WorldOperationResult(operation, WorldOperationResult.Status.REJECTED,
                REJECTED, Collections.<DefinitionId>emptyList(), true,
                provider.snapshot(operation.target())));
        return new CompletedHandle(result);
    }

    /** @return immutable operation accounting */
    public Snapshot accounting() {
        synchronized (active) {
            return new Snapshot(active.size(), maximumInFlight, accepted.get(), completed.get(),
                    failed.get(), cancelled.get(), rejected.get(), accepting.get());
        }
    }

    @Override public DefinitionId id() { return COMPONENT; }
    @Override public synchronized Result<Lifecycle.State> start(final Duration remainingBudget) {
        positive(remainingBudget);
        accepting.set(true);
        return Result.success(Lifecycle.State.RUNNING);
    }
    @Override public Result<Lifecycle.State> drain(final Duration remainingBudget) {
        positive(remainingBudget);
        accepting.set(false);
        final long now = System.nanoTime();
        final long duration = safeNanos(remainingBudget);
        final long deadline = duration > Long.MAX_VALUE - now ? Long.MAX_VALUE : now + duration;
        synchronized (active) {
            for (Execution execution : new ArrayList<Execution>(active.values())) { execution.cancel(); }
            while (!active.isEmpty()) {
                final long remaining = deadline - System.nanoTime();
                if (remaining <= 0L) {
                    return Result.failure(ApiError.of(DefinitionId.of("zartra", "world/drain_timeout"),
                            "world.drain.timeout", ApiError.RetryDisposition.RETRYABLE));
                }
                try {
                    TimeUnit.NANOSECONDS.timedWait(active, remaining);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    return Result.failure(ApiError.of(DefinitionId.of("zartra", "world/drain_interrupted"),
                            "world.drain.interrupted", ApiError.RetryDisposition.RETRYABLE));
                }
            }
        }
        return Result.success(Lifecycle.State.DRAINING);
    }
    @Override public Result<Lifecycle.State> stop(final Duration remainingBudget) {
        final Result<Lifecycle.State> drained = drain(remainingBudget);
        return drained.isSuccess() ? Result.success(Lifecycle.State.STOPPED) : drained;
    }
    @Override public Result<Lifecycle.State> forceStop() {
        accepting.set(false);
        synchronized (active) {
            for (Execution execution : new ArrayList<Execution>(active.values())) { execution.cancel(); }
        }
        return Result.success(Lifecycle.State.FORCED);
    }
    @Override public Health.Snapshot snapshot() {
        final Snapshot values = accounting();
        final Health.Status status = values.active() < values.maximumInFlight()
                ? Health.Status.HEALTHY : Health.Status.DEGRADED;
        return new Health.Snapshot(COMPONENT, status,
                status == Health.Status.HEALTHY ? DefinitionId.of("zartra", "world/ready")
                        : DefinitionId.of("zartra", "world/saturated"), timeSource.now());
    }

    private static void positive(final Duration duration) {
        if (Objects.requireNonNull(duration, "duration").isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("duration must be positive");
        }
    }
    private static long safeNanos(final Duration duration) {
        try { return duration.toNanos(); }
        catch (ArithmeticException exception) { return Long.MAX_VALUE; }
    }

    /** Thread-safe operation control handle. */
    public interface OperationHandle {
        /** @return eventual non-exceptional terminal result */ CompletionStage<WorldOperationResult> completion();
        /** @return true only when this call newly requested cancellation */ boolean cancel();
    }

    /** Immutable bounded accounting. */
    public static final class Snapshot {
        private final int active;
        private final int maximumInFlight;
        private final long accepted;
        private final long completed;
        private final long failed;
        private final long cancelled;
        private final long rejected;
        private final boolean accepting;
        private Snapshot(final int active, final int maximumInFlight, final long accepted,
                         final long completed, final long failed, final long cancelled,
                         final long rejected, final boolean accepting) {
            this.active = active;
            this.maximumInFlight = maximumInFlight;
            this.accepted = accepted;
            this.completed = completed;
            this.failed = failed;
            this.cancelled = cancelled;
            this.rejected = rejected;
            this.accepting = accepting;
        }
        /** @return active operations */ public int active() { return active; }
        /** @return hard admission bound */ public int maximumInFlight() { return maximumInFlight; }
        /** @return admitted count */ public long accepted() { return accepted; }
        /** @return successful count */ public long completed() { return completed; }
        /** @return failed or timed-out count */ public long failed() { return failed; }
        /** @return cancelled count */ public long cancelled() { return cancelled; }
        /** @return rejected count */ public long rejected() { return rejected; }
        /** @return admission state */ public boolean accepting() { return accepting; }
    }

    private final class Execution implements OperationHandle, CancellationToken {
        private final WorldProvider.Plan plan;
        private final CompletableFuture<WorldOperationResult> completion = new CompletableFuture<WorldOperationResult>();
        private final List<WorldProvider.Step> completedSteps = new ArrayList<WorldProvider.Step>();
        private final AtomicBoolean cancellation = new AtomicBoolean();
        private final AtomicBoolean rollbackStarted = new AtomicBoolean();
        private final AtomicBoolean terminal = new AtomicBoolean();
        private final long deadlineNanos;
        private volatile SchedulerPort.TaskHandle<?> current;
        private int index;
        private Execution(final WorldProvider.Plan plan) {
            this.plan = Objects.requireNonNull(plan, "plan");
            final long now = System.nanoTime();
            final long duration = safeNanos(plan.operation().timeout());
            this.deadlineNanos = duration > Long.MAX_VALUE - now ? Long.MAX_VALUE : now + duration;
        }
        @Override public CompletionStage<WorldOperationResult> completion() { return completion; }
        @Override public boolean isCancellationRequested() { return cancellation.get(); }
        @Override public boolean cancel() {
            if (terminal.get() || rollbackStarted.get()
                    || !cancellation.compareAndSet(false, true)) { return false; }
            final SchedulerPort.TaskHandle<?> task = current;
            beginRollback(WorldOperationResult.Status.CANCELLED, CANCELLED);
            if (task != null) { task.cancel(); }
            return true;
        }
        private void runNext() {
            if (rollbackStarted.get() || terminal.get()) { return; }
            if (isCancellationRequested()) {
                beginRollback(WorldOperationResult.Status.CANCELLED, CANCELLED);
                return;
            }
            if (System.nanoTime() >= deadlineNanos) {
                beginRollback(WorldOperationResult.Status.TIMED_OUT, SCHEDULER_TIMEOUT);
                return;
            }
            if (index >= plan.steps().size()) {
                finish(WorldOperationResult.Status.SUCCEEDED, SUCCEEDED, true);
                return;
            }
            final WorldProvider.Step step = plan.steps().get(index);
            execute(step, false).whenComplete((result, error) -> {
                if (rollbackStarted.get() || terminal.get()) { return; }
                if (error != null) { beginRollback(WorldOperationResult.Status.FAILED, INTERNAL); }
                else if (!result.isSuccess()) {
                    beginRollback(SCHEDULER_TIMEOUT.equals(result.reason())
                            ? WorldOperationResult.Status.TIMED_OUT
                            : WorldOperationResult.Status.FAILED, result.reason());
                }
                else {
                    completedSteps.add(step);
                    index++;
                    runNext();
                }
            });
        }
        private CompletionStage<WorldProvider.StepResult> execute(final WorldProvider.Step step,
                                                                  final boolean rollback) {
            if (step.affinity() == WorldProvider.Affinity.WORKER) {
                final SchedulerPort.TaskHandle<WorldProvider.StepResult> handle = scheduler.submit(
                        descriptor(step, rollback), context -> rollback
                                ? step.rollback(context.cancellationToken())
                                : step.execute(combined(context.cancellationToken())));
                current = handle;
                return handle.completion().thenApply(outcome -> outcome.value().orElseGet(() ->
                        WorldProvider.StepResult.failure(outcome.failure().map(FailureReport::code).orElse(INTERNAL))));
            }
            final AtomicReference<WorldProvider.StepResult> result = new AtomicReference<WorldProvider.StepResult>();
            final SchedulerPort.TaskHandle<Void> handle = ownerDispatcher.dispatch(
                    descriptor(step, rollback), () ->
                    result.set(rollback ? step.rollback(() -> false) : step.execute(this)));
            current = handle;
            return handle.completion().thenApply(outcome -> outcome.isSuccess()
                    ? Optional.ofNullable(result.get()).orElse(WorldProvider.StepResult.failure(INTERNAL))
                    : WorldProvider.StepResult.failure(outcome.failure().map(FailureReport::code).orElse(INTERNAL)));
        }
        private CancellationToken combined(final CancellationToken taskToken) {
            return () -> cancellation.get() || taskToken.isCancellationRequested();
        }
        private TaskDescriptor descriptor(final WorldProvider.Step step, final boolean rollback) {
            final long remaining = deadlineNanos - System.nanoTime();
            final Duration timeout = rollback ? Duration.ofSeconds(5)
                    : Duration.ofNanos(Math.max(1L, remaining));
            return TaskDescriptor.of(io.zartra.bedwars.api.identity.TaskId.random(), step.id(),
                    COMPONENT, plan.operation().correlationId(), timeout, true);
        }
        private void beginRollback(final WorldOperationResult.Status status,
                                   final DefinitionId reason) {
            if (terminal.get() || !rollbackStarted.compareAndSet(false, true)) { return; }
            rollback(status, reason, completedSteps.size() - 1, true);
        }
        private void rollback(final WorldOperationResult.Status status, final DefinitionId reason,
                              final int rollbackIndex, final boolean rollbackComplete) {
            if (terminal.get()) { return; }
            if (rollbackIndex < 0) {
                finish(status, reason, rollbackComplete);
                return;
            }
            execute(completedSteps.get(rollbackIndex), true).whenComplete((result, error) ->
                    rollback(status, reason, rollbackIndex - 1,
                            rollbackComplete && error == null && result.isSuccess()));
        }
        private void finish(final WorldOperationResult.Status status, final DefinitionId reason,
                            final boolean rollbackComplete) {
            if (!terminal.compareAndSet(false, true)) { return; }
            final List<DefinitionId> ids = new ArrayList<DefinitionId>();
            for (WorldProvider.Step step : completedSteps) { ids.add(step.id()); }
            if (status == WorldOperationResult.Status.SUCCEEDED) { completed.incrementAndGet(); }
            else if (status == WorldOperationResult.Status.CANCELLED) { cancelled.incrementAndGet(); }
            else { failed.incrementAndGet(); }
            final WorldOperationResult result = new WorldOperationResult(plan.operation(), status,
                    reason, ids, rollbackComplete, provider.snapshot(plan.operation().target()));
            synchronized (active) {
                active.remove(plan.operation().operationId().toString());
                activeWorlds.remove(plan.operation().target());
                active.notifyAll();
            }
            completion.complete(result);
        }
    }

    private static final class CompletedHandle implements OperationHandle {
        private final CompletionStage<WorldOperationResult> completion;
        private CompletedHandle(final CompletionStage<WorldOperationResult> completion) { this.completion = completion; }
        @Override public CompletionStage<WorldOperationResult> completion() { return completion; }
        @Override public boolean cancel() { return false; }
    }
}
