package io.zartra.bedwars.paper.bootstrap;

import io.zartra.bedwars.api.failure.FailureSink;
import io.zartra.bedwars.api.lifecycle.Lifecycle;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.api.time.MonotonicTimeSource;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.application.scheduler.BoundedTaskScheduler;
import io.zartra.bedwars.compat.modern.Paper121CompatibilityAdapter;
import io.zartra.bedwars.paper.world.PaperNativeWorldProvider;
import io.zartra.bedwars.world.api.WorldOperation;
import io.zartra.bedwars.world.orchestration.WorldOrchestrator;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.plugin.java.JavaPlugin;

/** Bounded M06 composition root for the primary Paper runtime. */
public final class PaperFoundationRuntime {
    private static final Class<?> SHUTDOWN_REPORT_TYPE = BoundedTaskScheduler.ShutdownReport.class;
    private final BoundedTaskScheduler workerScheduler;
    private final Paper121CompatibilityAdapter compatibility;
    private final WorldOrchestrator worlds;
    private final ThreadPoolExecutor shutdownExecutor;
    private final CompletableFuture<Boolean> stopped = new CompletableFuture<Boolean>();
    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean stopRequested = new AtomicBoolean();

    /** Creates but does not start the primary foundation. */
    public PaperFoundationRuntime(final JavaPlugin plugin, final Path worldRoot,
                                  final PaperFoundationSettings settings,
                                  final TimeSource timeSource) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(settings, "settings");
        Objects.requireNonNull(SHUTDOWN_REPORT_TYPE, "shutdownReportType");
        final TimeSource clock = Objects.requireNonNull(timeSource, "timeSource");
        final FailureSink sink = report -> plugin.getLogger().warning(
                report.code() + " correlation=" + report.correlationId());
        workerScheduler = new BoundedTaskScheduler(settings.workers(), settings.queueCapacity(),
                "zbw-world", MonotonicTimeSource.SystemMonotonicTimeSource.INSTANCE, clock, sink);
        compatibility = new Paper121CompatibilityAdapter(clock);
        worlds = new WorldOrchestrator(workerScheduler,
                new PaperOwnerThreadDispatcher(plugin, clock),
                new PaperNativeWorldProvider(worldRoot, settings.maximumTrackedWorlds()),
                clock, settings.maximumInFlightWorlds());
        shutdownExecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<Runnable>(1), new ShutdownThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy());
    }

    /** Starts compatibility and world admission; must be called once during Paper enable. */
    public synchronized void start() {
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("Paper foundation already started");
        }
        final Result<Lifecycle.State> worldState = worlds.start(Duration.ofSeconds(5));
        if (!worldState.isSuccess()) { throw new IllegalStateException("world foundation start failed"); }
        if (!compatibility.start().toCompletableFuture().join().isSuccess()) {
            worlds.forceStop();
            throw new IllegalStateException("compatibility foundation start failed");
        }
    }

    /** @return admitted or typed-rejected bounded world operation */
    public WorldOrchestrator.OperationHandle submit(final WorldOperation operation) {
        if (!started.get() || stopRequested.get()) {
            throw new IllegalStateException("Paper foundation is not accepting operations");
        }
        return worlds.submit(operation);
    }

    /**
     * Initiates non-blocking shutdown. Paper's owner thread never waits for worker termination.
     *
     * @return eventual worker termination evidence
     */
    public CompletionStage<Boolean> stop() {
        if (!stopRequested.compareAndSet(false, true)) { return stopped; }
        worlds.forceStop();
        compatibility.stop();
        workerScheduler.stopAdmission();
        shutdownExecutor.execute(() -> {
            try {
                final boolean terminated = workerScheduler.shutdown(
                        Duration.ofSeconds(5), Duration.ofSeconds(2)).terminated();
                stopped.complete(Boolean.valueOf(terminated));
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                stopped.complete(Boolean.FALSE);
            } finally {
                shutdownExecutor.shutdown();
            }
        });
        return stopped;
    }

    /** @return immutable world-operation accounting */
    public WorldOrchestrator.Snapshot accounting() { return worlds.accounting(); }

    /** @return exact primary compatibility adapter */
    public Paper121CompatibilityAdapter compatibility() { return compatibility; }

    private static final class ShutdownThreadFactory implements ThreadFactory {
        @Override public Thread newThread(final Runnable runnable) {
            final Thread thread = new Thread(runnable, "zbw-foundation-shutdown");
            thread.setDaemon(true);
            return thread;
        }
    }
}
