package io.zartra.bedwars.paper.game;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/** Bounded, cancellable, timed and drainable off-owner executor for pure matching calculations. */
public final class BoundedMatchmakingExecutor implements AutoCloseable {
    private final ThreadPoolExecutor workers;
    private final ScheduledThreadPoolExecutor deadlines;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicInteger inFlight = new AtomicInteger();
    private final AtomicInteger timedOut = new AtomicInteger();
    private final AtomicInteger rejected = new AtomicInteger();

    /** Creates fixed named daemon workers and a bounded queue. */
    public BoundedMatchmakingExecutor(final int workerCount, final int queueCapacity,
                                     final String threadPrefix) {
        if (workerCount < 1 || workerCount > 32 || queueCapacity < 1 || queueCapacity > 100000
                || threadPrefix == null || !threadPrefix.matches("[A-Za-z0-9_.-]{1,64}")) {
            throw new IllegalArgumentException("invalid matchmaking executor bounds");
        }
        workers = new ThreadPoolExecutor(workerCount, workerCount, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueCapacity), factory(threadPrefix),
                new ThreadPoolExecutor.AbortPolicy());
        deadlines = new ScheduledThreadPoolExecutor(1, factory(threadPrefix + "-deadline"));
        deadlines.setRemoveOnCancelPolicy(true);
    }

    /** Submits pure work and fails the future when its bounded timeout elapses. */
    public <T> CompletableFuture<T> submit(final Supplier<T> work, final Duration timeout) {
        Objects.requireNonNull(work, "work");
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        if (closed.get()) { rejected.incrementAndGet();
        throw new RejectedExecutionException("matchmaking executor closed");
        }
        final CompletableFuture<T> result = new CompletableFuture<>();
        final AtomicBoolean terminal = new AtomicBoolean();
        inFlight.incrementAndGet();
        final var deadline = deadlines.schedule(() -> {
            if (terminal.compareAndSet(false, true)) {
                timedOut.incrementAndGet();
                inFlight.decrementAndGet();
                result.completeExceptionally(new java.util.concurrent.TimeoutException("matchmaking calculation timed out"));
            }
        }, timeout.toMillis(), TimeUnit.MILLISECONDS);
        try {
            workers.execute(() -> {
                try {
                    final T value = work.get();
                    if (terminal.compareAndSet(false, true)) {
                        deadline.cancel(false);
                        inFlight.decrementAndGet();
                        result.complete(value);
                    }
                } catch (RuntimeException failure) {
                    if (terminal.compareAndSet(false, true)) {
                        deadline.cancel(false);
                        inFlight.decrementAndGet();
                        result.completeExceptionally(failure);
                    }
                }
            });
        } catch (RejectedExecutionException failure) {
            deadline.cancel(false);
            inFlight.decrementAndGet();
            rejected.incrementAndGet();
            throw failure;
        }
        return result;
    }

    /** @return executing plus queued operations */ public int inFlight() { return inFlight.get(); }
    /** @return queued operations */ public int queued() { return workers.getQueue().size(); }
    /** @return timeout count */ public int timedOut() { return timedOut.get(); }
    /** @return admission rejection count */ public int rejected() { return rejected.get(); }
    /** @return shutdown state */ public boolean isClosed() { return closed.get(); }

    /** Stops admission and attempts a bounded drain. */
    public boolean close(final Duration timeout) {
        if (timeout == null || timeout.isNegative()) { throw new IllegalArgumentException("timeout must not be negative"); }
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
    @Override public void close() { close(Duration.ofSeconds(5)); }

    private static ThreadFactory factory(final String prefix) {
        final AtomicInteger sequence = new AtomicInteger();
        return runnable -> {
            final Thread thread = new Thread(runnable, prefix + '-' + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }
}
