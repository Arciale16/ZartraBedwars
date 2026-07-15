package io.zartra.bedwars.api.scheduler;

import io.zartra.bedwars.api.failure.FailureReport;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.TaskId;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/** Platform-neutral port for explicitly bounded asynchronous work. */
public interface SchedulerPort {
    /** @return handle for an admitted or typed-rejected task; never null */
    <T> TaskHandle<T> submit(TaskDescriptor descriptor, TaskOperation<T> operation);
    /** Stops admission without discarding already accepted work. */ void stopAdmission();
    /** @return current immutable queue and outcome accounting */ Snapshot snapshot();

    /** Functional operation executed by a bounded scheduler. */
    interface TaskOperation<T> {
        /** @param context immutable context @return non-null result @throws Exception as structured failure */
        T execute(TaskContext context) throws Exception;
    }

    /** Thread-safe control handle. */
    interface TaskHandle<T> {
        /** @return task identity */ TaskId taskId();
        /** @return eventual outcome; never exceptional */ CompletionStage<Outcome<T>> completion();
        /** @return true only when this call newly requested cancellation */ boolean cancel();
    }

    /** Immutable success-or-structured-failure result. */
    final class Outcome<T> {
        private final T value;
        private final FailureReport failure;
        private Outcome(final T value, final FailureReport failure) {
            this.value = value;
            this.failure = failure;
        }
        /** @return successful outcome */
        public static <T> Outcome<T> success(final T value) {
            return new Outcome<T>(Objects.requireNonNull(value, "value"), null);
        }
        /** @return successful completion for a mutation that has no value */
        public static Outcome<Void> successVoid() {
            return new Outcome<Void>(null, null);
        }
        /** @return failed outcome */
        public static <T> Outcome<T> failure(final FailureReport failure) {
            return new Outcome<T>(null, Objects.requireNonNull(failure, "failure"));
        }
        /** @return whether value exists */ public boolean isSuccess() { return failure == null; }
        /** @return optional value */ public Optional<T> value() { return Optional.ofNullable(value); }
        /** @return optional failure */ public Optional<FailureReport> failure() { return Optional.ofNullable(failure); }
    }

    /** Immutable bounded-executor accounting snapshot. */
    final class Snapshot {
        private final int active;
        private final int queued;
        private final long accepted;
        private final long completed;
        private final long failed;
        private final long rejected;
        private final long cancelled;
        private final boolean accepting;
        /** Creates a validated snapshot. */
        public Snapshot(final int active, final int queued, final long accepted,
                        final long completed, final long failed, final long rejected,
                        final long cancelled, final boolean accepting) {
            if (active < 0 || queued < 0 || accepted < 0 || completed < 0 || failed < 0
                    || rejected < 0 || cancelled < 0) {
                throw new IllegalArgumentException("scheduler counters cannot be negative");
            }
            this.active = active;
            this.queued = queued;
            this.accepted = accepted;
            this.completed = completed;
            this.failed = failed;
            this.rejected = rejected;
            this.cancelled = cancelled;
            this.accepting = accepting;
        }
        /** @return active count */ public int active() { return active; }
        /** @return queued count */ public int queued() { return queued; }
        /** @return accepted count */ public long accepted() { return accepted; }
        /** @return successful count */ public long completed() { return completed; }
        /** @return failure count */ public long failed() { return failed; }
        /** @return rejection count */ public long rejected() { return rejected; }
        /** @return cancellation count */ public long cancelled() { return cancelled; }
        /** @return admission state */ public boolean accepting() { return accepting; }
    }

    /** Platform adapter contract for owner-context dispatch. */
    interface OwnerThreadDispatcher {
        /** @return whether caller owns context */ boolean isOwnerThread(DefinitionId ownerId);
        /** @return owner-dispatch handle */ TaskHandle<Void> dispatch(TaskDescriptor descriptor, Runnable mutation);
    }

    /** Fail-fast guard for thread-affinity boundaries. */
    interface ThreadGuard {
        /** @throws ThreadAccessException off owner */ void requireOwnerThread(DefinitionId ownerId);
        /** @throws ThreadAccessException on owner */ void requireWorkerThread(DefinitionId ownerId);
    }

    /** Typed programmer error for an illegal boundary crossing. */
    final class ThreadAccessException extends IllegalStateException {
        private static final long serialVersionUID = 1L;
        private final String ownerId;
        /** Creates a secret-safe affinity failure. */
        public ThreadAccessException(final DefinitionId ownerId, final String message) {
            super(Objects.requireNonNull(message, "message"));
            this.ownerId = Objects.requireNonNull(ownerId, "ownerId").toString();
        }
        /** @return violated owner */ public DefinitionId ownerId() { return DefinitionId.parse(ownerId); }
    }
}
