package io.zartra.bedwars.cloudnet;

import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Bounded worker used for CloudNet callbacks.
 *
 * <p>ZBW-ADDON-233: rejection is immediate and work never falls back to the caller, preventing
 * accidental execution on a Paper owner thread.</p>
 */
public final class BoundedCloudExecutor implements AutoCloseable {
    private final ThreadPoolExecutor executor;

    /**
     * Creates a bounded daemon worker.
     *
     * @param workers worker count from one to eight
     * @param queueCapacity queue capacity from one to 1024
     */
    public BoundedCloudExecutor(final int workers, final int queueCapacity) {
        if (workers < 1 || workers > 8 || queueCapacity < 1 || queueCapacity > 1024) {
            throw new IllegalArgumentException("invalid worker or queue bound");
        }
        executor = new ThreadPoolExecutor(
                workers, workers, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<Runnable>(queueCapacity),
                new CloudThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy());
    }

    /**
     * Runs an operation on the bounded worker.
     *
     * @param operation nonblocking operation supplier
     * @param <T> result type
     * @return flattened asynchronous result
     */
    public <T> CompletionStage<T> submit(final Supplier<CompletionStage<T>> operation) {
        Objects.requireNonNull(operation, "operation");
        final CompletableFuture<T> result = new CompletableFuture<T>();
        try {
            executor.execute(() -> {
                try {
                    operation.get().whenComplete((value, failure) -> {
                        if (failure == null) {
                            result.complete(value);
                        } else {
                            result.completeExceptionally(failure);
                        }
                    });
                } catch (RuntimeException failure) {
                    result.completeExceptionally(failure);
                }
            });
        } catch (RejectedExecutionException failure) {
            result.completeExceptionally(failure);
        }
        return result;
    }

    /** @return current bounded queue depth for diagnostics */
    public int queueDepth() { return executor.getQueue().size(); }

    @Override public void close() { executor.shutdownNow(); }

    private static final class CloudThreadFactory implements ThreadFactory {
        private int sequence;
        @Override public synchronized Thread newThread(final Runnable runnable) {
            final Thread thread = new Thread(runnable, "zbw-cloudnet-" + ++sequence);
            thread.setDaemon(true);
            return thread;
        }
    }
}
