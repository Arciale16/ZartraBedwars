package io.zartra.bedwars.paper.bootstrap;

import io.zartra.bedwars.api.failure.FailureKind;
import io.zartra.bedwars.api.failure.FailureReport;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.TaskId;
import io.zartra.bedwars.api.scheduler.SchedulerPort;
import io.zartra.bedwars.api.scheduler.TaskDescriptor;
import io.zartra.bedwars.api.time.TimeSource;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/** Paper scheduler adapter that confines all platform mutations to the primary thread. */
public final class PaperOwnerThreadDispatcher implements SchedulerPort.OwnerThreadDispatcher {
    private static final DefinitionId CANCELLED = DefinitionId.of("zartra", "paper/dispatch_cancelled");
    private static final DefinitionId FAILED = DefinitionId.of("zartra", "paper/dispatch_failed");
    private static final DefinitionId TIMED_OUT = DefinitionId.of("zartra", "paper/dispatch_timeout");
    private final JavaPlugin plugin;
    private final TimeSource timeSource;

    /** Creates a dispatcher owned by one enabled plugin. */
    public PaperOwnerThreadDispatcher(final JavaPlugin plugin, final TimeSource timeSource) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.timeSource = Objects.requireNonNull(timeSource, "timeSource");
    }

    @Override public boolean isOwnerThread(final DefinitionId ownerId) {
        Objects.requireNonNull(ownerId, "ownerId");
        return Bukkit.isPrimaryThread();
    }

    @Override public SchedulerPort.TaskHandle<Void> dispatch(final TaskDescriptor descriptor,
                                                             final Runnable mutation) {
        return new PaperTaskHandle(descriptor, mutation);
    }

    private final class PaperTaskHandle implements SchedulerPort.TaskHandle<Void> {
        private final TaskDescriptor descriptor;
        private final CompletableFuture<SchedulerPort.Outcome<Void>> completion =
                new CompletableFuture<SchedulerPort.Outcome<Void>>();
        private final AtomicBoolean terminal = new AtomicBoolean();
        private final long deadlineNanos;
        private final BukkitTask task;
        PaperTaskHandle(final TaskDescriptor descriptor, final Runnable mutation) {
            this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
            Objects.requireNonNull(mutation, "mutation");
            final long now = System.nanoTime();
            final long timeout;
            try {
                timeout = descriptor.timeout().toNanos();
            } catch (ArithmeticException exception) {
                deadlineNanos = Long.MAX_VALUE;
                task = schedule(mutation);
                return;
            }
            deadlineNanos = timeout > Long.MAX_VALUE - now ? Long.MAX_VALUE : now + timeout;
            task = schedule(mutation);
        }
        private BukkitTask schedule(final Runnable mutation) {
            return Bukkit.getScheduler().runTask(plugin, () -> {
                if (System.nanoTime() >= deadlineNanos) {
                    if (terminal.compareAndSet(false, true)) {
                        completion.complete(SchedulerPort.Outcome.failure(report(TIMED_OUT,
                                FailureKind.TIMEOUT, "paper.dispatch.timeout", true)));
                    }
                    return;
                }
                if (!terminal.compareAndSet(false, true)) { return; }
                try {
                    mutation.run();
                    completion.complete(SchedulerPort.Outcome.successVoid());
                } catch (RuntimeException | LinkageError failure) {
                    completion.complete(SchedulerPort.Outcome.failure(report(FAILED,
                            FailureKind.INTERNAL, "paper.dispatch.failed", false)));
                }
            });
        }
        @Override public TaskId taskId() { return descriptor.taskId(); }
        @Override public CompletionStage<SchedulerPort.Outcome<Void>> completion() { return completion; }
        @Override public boolean cancel() {
            if (!terminal.compareAndSet(false, true)) { return false; }
            task.cancel();
            completion.complete(SchedulerPort.Outcome.failure(report(CANCELLED,
                    FailureKind.REJECTED, "paper.dispatch.cancelled", false)));
            return true;
        }
        private FailureReport report(final DefinitionId code, final FailureKind kind,
                                     final String message, final boolean retryable) {
            return FailureReport.of(code, kind, descriptor.correlationId(), message,
                    retryable, timeSource.now());
        }
    }
}
