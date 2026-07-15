package io.zartra.bedwars.application.scheduler;

import io.zartra.bedwars.api.failure.FailureKind;
import io.zartra.bedwars.api.failure.FailureReport;
import io.zartra.bedwars.api.failure.FailureSink;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.TaskId;
import io.zartra.bedwars.api.scheduler.CancellationToken;
import io.zartra.bedwars.api.scheduler.SchedulerPort;
import io.zartra.bedwars.api.scheduler.TaskContext;
import io.zartra.bedwars.api.scheduler.TaskDescriptor;
import io.zartra.bedwars.api.time.MonotonicTimeSource;
import io.zartra.bedwars.api.time.TimeSource;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/** Fixed-size scheduler with bounded admission, cooperative deadlines and loss-accounted shutdown. */
public final class BoundedTaskScheduler implements SchedulerPort {
    private static final DefinitionId REJECTED = DefinitionId.of("zartra", "scheduler/rejected");
    private static final DefinitionId CANCELLED = DefinitionId.of("zartra", "scheduler/cancelled");
    private static final DefinitionId TIMEOUT = DefinitionId.of("zartra", "scheduler/timeout");
    private static final DefinitionId INTERNAL = DefinitionId.of("zartra", "scheduler/internal");
    private final ThreadPoolExecutor executor;
    private final MonotonicTimeSource monotonic;
    private final TimeSource wallClock;
    private final FailureSink failureSink;
    private final AtomicBoolean accepting = new AtomicBoolean(true);
    private final AtomicLong accepted = new AtomicLong();
    private final AtomicLong completed = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();
    private final AtomicLong rejected = new AtomicLong();
    private final AtomicLong cancelled = new AtomicLong();

    /** Creates an active scheduler with exact worker and queue capacities. */
    public BoundedTaskScheduler(final int workers, final int queueCapacity, final String threadPrefix,
                                final MonotonicTimeSource monotonic, final TimeSource wallClock,
                                final FailureSink failureSink) {
        if (workers < 1 || queueCapacity < 1) {
            throw new IllegalArgumentException("workers and queueCapacity must be positive");
        }
        if (threadPrefix == null || !threadPrefix.matches("[a-zA-Z0-9_.-]{1,48}")) {
            throw new IllegalArgumentException("threadPrefix must be a safe bounded label");
        }
        this.monotonic = Objects.requireNonNull(monotonic, "monotonic");
        this.wallClock = Objects.requireNonNull(wallClock, "wallClock");
        this.failureSink = Objects.requireNonNull(failureSink, "failureSink");
        this.executor = new ThreadPoolExecutor(workers, workers, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<Runnable>(queueCapacity), new NamedThreadFactory(threadPrefix),
                new ThreadPoolExecutor.AbortPolicy());
    }

    @Override public <T> TaskHandle<T> submit(final TaskDescriptor descriptor,
                                               final TaskOperation<T> operation) {
        Objects.requireNonNull(descriptor, "descriptor");
        Objects.requireNonNull(operation, "operation");
        final Runner<T> runner = new Runner<T>(descriptor, operation, deadline(descriptor.timeout()));
        if (!accepting.get()) {
            runner.reject();
            return runner;
        }
        try {
            executor.execute(runner);
            accepted.incrementAndGet();
        } catch (RejectedExecutionException exception) {
            runner.reject();
        }
        return runner;
    }

    @Override public void stopAdmission() { accepting.set(false); }

    @Override public Snapshot snapshot() {
        return new Snapshot(executor.getActiveCount(), executor.getQueue().size(),
                accepted.get(), completed.get(), failed.get(), rejected.get(), cancelled.get(),
                accepting.get());
    }

    /**
     * Drains accepted work and then interrupts remaining tasks. This blocking control operation
     * must run outside every Minecraft owner thread.
     */
    public ShutdownReport shutdown(final Duration gracefulBudget, final Duration forcedBudget)
            throws InterruptedException {
        positive(gracefulBudget, "gracefulBudget");
        positive(forcedBudget, "forcedBudget");
        stopAdmission();
        executor.shutdown();
        final boolean forced = !executor.awaitTermination(
                toNanos(gracefulBudget), TimeUnit.NANOSECONDS);
        int forceCancelled = 0;
        if (forced) {
            final List<Runnable> abandoned = executor.shutdownNow();
            for (Runnable runnable : abandoned) {
                if (((Runner<?>) runnable).cancelFromShutdown()) { forceCancelled++; }
            }
            executor.awaitTermination(toNanos(forcedBudget), TimeUnit.NANOSECONDS);
        }
        return new ShutdownReport(snapshot(), forced, forceCancelled, executor.isTerminated());
    }

    private static void positive(final Duration duration, final String label) {
        Objects.requireNonNull(duration, label);
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(label + " must be positive");
        }
    }

    private long deadline(final Duration timeout) {
        final long now = monotonic.readNanos();
        final long delta = toNanos(timeout);
        return delta > Long.MAX_VALUE - now ? Long.MAX_VALUE : now + delta;
    }

    private static long toNanos(final Duration duration) {
        try { return duration.toNanos(); }
        catch (ArithmeticException exception) { return Long.MAX_VALUE; }
    }

    private FailureReport failure(final TaskDescriptor descriptor, final DefinitionId code,
                                  final FailureKind kind, final String messageKey,
                                  final boolean retryable) {
        return FailureReport.of(code, kind, descriptor.correlationId(), messageKey,
                retryable && descriptor.idempotent(), wallClock.now());
    }

    private void publish(final FailureReport report) {
        try { failureSink.publish(report); }
        catch (RuntimeException ignored) { return; }
    }

    /** Immutable result of bounded shutdown. */
    public static final class ShutdownReport {
        private final Snapshot snapshot;
        private final boolean forced;
        private final int forceCancelled;
        private final boolean terminated;
        private ShutdownReport(final Snapshot snapshot, final boolean forced,
                               final int forceCancelled, final boolean terminated) {
            this.snapshot = snapshot;
            this.forced = forced;
            this.forceCancelled = forceCancelled;
            this.terminated = terminated;
        }
        /** @return final accounting */ public Snapshot snapshot() { return snapshot; }
        /** @return whether graceful drain elapsed */ public boolean forced() { return forced; }
        /** @return queued tasks explicitly cancelled */ public int forceCancelled() { return forceCancelled; }
        /** @return whether all workers terminated */ public boolean terminated() { return terminated; }
    }

    private final class Runner<T> implements Runnable, TaskHandle<T>, CancellationToken {
        private final TaskDescriptor descriptor;
        private final TaskOperation<T> operation;
        private final long deadlineNanos;
        private final CompletableFuture<Outcome<T>> completion = new CompletableFuture<Outcome<T>>();
        private final AtomicBoolean cancellation = new AtomicBoolean();
        private volatile Thread runningThread;
        private Runner(final TaskDescriptor descriptor, final TaskOperation<T> operation,
                       final long deadlineNanos) {
            this.descriptor = descriptor;
            this.operation = operation;
            this.deadlineNanos = deadlineNanos;
        }
        @Override public TaskId taskId() { return descriptor.taskId(); }
        @Override public CompletionStage<Outcome<T>> completion() { return completion; }
        @Override public boolean isCancellationRequested() {
            return cancellation.get() || monotonic.readNanos() >= deadlineNanos;
        }
        @Override public boolean cancel() {
            if (completion.isDone() || !cancellation.compareAndSet(false, true)) { return false; }
            executor.remove(this);
            final Thread thread = runningThread;
            if (thread != null) { thread.interrupt(); }
            completeFailure(failure(descriptor, CANCELLED, FailureKind.REJECTED,
                    "scheduler.task.cancelled", false), true);
            return true;
        }
        private boolean cancelFromShutdown() {
            if (completion.isDone() || !cancellation.compareAndSet(false, true)) { return false; }
            completeFailure(failure(descriptor, CANCELLED, FailureKind.REJECTED,
                    "scheduler.task.shutdown", false), true);
            return true;
        }
        private void reject() {
            rejected.incrementAndGet();
            completeFailure(failure(descriptor, REJECTED, FailureKind.REJECTED,
                    "scheduler.task.rejected", true), false);
        }
        @Override public void run() {
            if (completion.isDone()) { return; }
            runningThread = Thread.currentThread();
            try {
                if (isCancellationRequested()) {
                    completeFailure(failure(descriptor, TIMEOUT, FailureKind.TIMEOUT,
                            "scheduler.task.timeout", true), false);
                    return;
                }
                final T value = Objects.requireNonNull(
                        operation.execute(new TaskContext(descriptor, this)), "operation result");
                if (isCancellationRequested()) {
                    completeFailure(failure(descriptor, TIMEOUT, FailureKind.TIMEOUT,
                            "scheduler.task.timeout", true), false);
                } else if (completion.complete(Outcome.success(value))) {
                    completed.incrementAndGet();
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                completeFailure(failure(descriptor, CANCELLED, FailureKind.REJECTED,
                        "scheduler.task.interrupted", true), true);
            } catch (Exception exception) {
                completeFailure(failure(descriptor, INTERNAL, FailureKind.INTERNAL,
                        "scheduler.task.failed", false), false);
            } catch (LinkageError error) {
                completeFailure(failure(descriptor, INTERNAL, FailureKind.INTERNAL,
                        "scheduler.task.linkage", false), false);
            } finally {
                runningThread = null;
            }
        }
        private void completeFailure(final FailureReport report, final boolean cancellationOutcome) {
            if (completion.complete(Outcome.<T>failure(report))) {
                if (cancellationOutcome) { cancelled.incrementAndGet(); }
                else if (report.kind() != FailureKind.REJECTED) { failed.incrementAndGet(); }
                publish(report);
            }
        }
    }

    private static final class NamedThreadFactory implements ThreadFactory {
        private final String prefix;
        private final AtomicLong sequence = new AtomicLong();
        private NamedThreadFactory(final String prefix) { this.prefix = prefix; }
        @Override public Thread newThread(final Runnable runnable) {
            final Thread thread = new Thread(runnable, prefix + "-" + sequence.incrementAndGet());
            thread.setDaemon(true);
            thread.setUncaughtExceptionHandler((ignoredThread, ignoredFailure) -> { });
            return thread;
        }
    }
}
