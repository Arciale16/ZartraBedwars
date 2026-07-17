package io.zartra.bedwars.command.paper;

import io.zartra.bedwars.command.api.CommandFramework;
import io.zartra.bedwars.command.api.CommandModel;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** Bounded, observable and drainable worker supervisor for command use cases. */
public final class BoundedCommandSupervisor implements CommandFramework.ExecutionSupervisor, AutoCloseable {
    private final ThreadPoolExecutor workers;
    private final ScheduledThreadPoolExecutor deadlines;
    private final AtomicInteger inFlight = new AtomicInteger();
    private final AtomicBoolean closed = new AtomicBoolean();

    /** Creates named daemon workers and a bounded rejection queue. */
    public BoundedCommandSupervisor(final int workerCount, final int queueCapacity,
                                    final String threadPrefix) {
        if (workerCount < 1 || workerCount > 64 || queueCapacity < 1 || queueCapacity > 100000) {
            throw new IllegalArgumentException("invalid supervisor bounds");
        }
        final ThreadFactory factory = namedFactory(threadPrefix);
        workers = new ThreadPoolExecutor(workerCount, workerCount, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<Runnable>(queueCapacity), factory,
                new ThreadPoolExecutor.AbortPolicy());
        deadlines = new ScheduledThreadPoolExecutor(1, namedFactory(threadPrefix + "-deadline"));
        deadlines.setRemoveOnCancelPolicy(true);
    }

    @Override public CompletionStage<CommandModel.Result> submit(final CommandModel.Node node,
                                                                 final CommandModel.ExecutionContext context,
                                                                 final CommandModel.Executor executor) {
        Objects.requireNonNull(node, "node");
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(executor, "executor");
        if (closed.get()) { throw new RejectedExecutionException("command supervisor closed"); }
        final CompletableFuture<CommandModel.Result> result = new CompletableFuture<CommandModel.Result>();
        final AtomicBoolean terminal = new AtomicBoolean();
        inFlight.incrementAndGet();
        final long delay = Math.max(0L, Duration.between(java.time.Instant.now(), context.deadline()).toMillis());
        final java.util.concurrent.ScheduledFuture<?> timeout = deadlines.schedule(() -> {
            if (terminal.compareAndSet(false, true)) {
                inFlight.decrementAndGet();
                result.complete(CommandModel.Result.simple(CommandModel.Result.Status.TIMEOUT,
                        "command.execution.timeout"));
            }
        }, delay, TimeUnit.MILLISECONDS);
        try {
            workers.execute(() -> {
                if (terminal.get()) { return; }
                final CompletionStage<CommandModel.Result> stage;
                try { stage = Objects.requireNonNull(executor.execute(context), "executor result"); }
                catch (RuntimeException failure) { complete(result, terminal, timeout, null, failure);
                 return;
                }
                stage.whenComplete((value, failure) -> complete(result, terminal, timeout, value, failure));
            });
        } catch (RejectedExecutionException failure) {
            timeout.cancel(false);
             inFlight.decrementAndGet();
            throw failure;
        }
        return result;
    }

    private void complete(final CompletableFuture<CommandModel.Result> result,
                          final AtomicBoolean terminal,
                          final java.util.concurrent.ScheduledFuture<?> timeout,
                          final CommandModel.Result value, final Throwable failure) {
        if (!terminal.compareAndSet(false, true)) { return; }
        timeout.cancel(false);
        inFlight.decrementAndGet();
        result.complete(failure == null && value != null ? value
                : CommandModel.Result.simple(CommandModel.Result.Status.ERROR,
                "command.execution.failed"));
    }

    /** @return current submitted but incomplete operation count */ public int inFlight() { return inFlight.get(); }
    /** @return queued worker count */ public int queued() { return workers.getQueue().size(); }
    /** @return whether shutdown began */ public boolean isClosed() { return closed.get(); }

    /** Stops admission and attempts a bounded graceful drain before interruption. */
    public boolean close(final Duration timeout) {
        Objects.requireNonNull(timeout, "timeout");
        if (timeout.isNegative()) { throw new IllegalArgumentException("timeout must not be negative"); }
        closed.set(true);
         workers.shutdown();
        deadlines.shutdown();
        try {
            final boolean drained = workers.awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!drained) { workers.shutdownNow(); }
            deadlines.shutdownNow();
            return drained && inFlight.get() == 0;
        } catch (InterruptedException interruption) {
            Thread.currentThread().interrupt();
             workers.shutdownNow();
             deadlines.shutdownNow();
            return false;
        }
    }
    @Override public void close() { close(Duration.ofSeconds(5L)); }

    private static ThreadFactory namedFactory(final String prefix) {
        if (prefix == null || !prefix.matches("[a-zA-Z0-9_.-]{1,64}")) { throw new IllegalArgumentException("invalid thread prefix"); }
        final AtomicInteger sequence = new AtomicInteger();
        return runnable -> { final Thread thread = new Thread(runnable, prefix + '-' + sequence.incrementAndGet());
         thread.setDaemon(true);
         return thread;
        };
    }
}
