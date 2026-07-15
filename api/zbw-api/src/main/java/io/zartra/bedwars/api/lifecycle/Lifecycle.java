package io.zartra.bedwars.api.lifecycle;

import io.zartra.bedwars.api.failure.FailureReport;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.result.Result;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Lifecycle contracts for startup, admission stop, drain, shutdown and force-stop. */
public final class Lifecycle {
    private Lifecycle() { throw new AssertionError("No instances"); }
    /** Stable component state. */
    public enum State {
        /** Not started. */ NEW,
        /** Ready. */ RUNNING,
        /** Admission stopped while accepted work drains. */ DRAINING,
        /** Gracefully stopped. */ STOPPED,
        /** Force-stopped. */ FORCED,
        /** Transition failed. */ FAILED
    }
    /** Component invoked only by a dedicated lifecycle worker. Methods must be idempotent. */
    public interface Component {
        /** @return stable component ID */ DefinitionId id();
        /** @return startup result */ Result<State> start(Duration remainingBudget);
        /** @return drain result */ Result<State> drain(Duration remainingBudget);
        /** @return stop result */ Result<State> stop(Duration remainingBudget);
        /** @return bounded force-stop result */ Result<State> forceStop();
    }
    /** Immutable outcome of a lifecycle transition. */
    public static final class Report {
        private final State state;
        private final List<DefinitionId> completed;
        private final List<FailureReport> failures;
        private final boolean forced;
        /** Creates a deterministic report. */
        public Report(final State state, final List<DefinitionId> completed,
                      final List<FailureReport> failures, final boolean forced) {
            this.state = Objects.requireNonNull(state, "state");
            final List<DefinitionId> ids = new ArrayList<DefinitionId>(
                    Objects.requireNonNull(completed, "completed"));
            final List<FailureReport> errors = new ArrayList<FailureReport>(
                    Objects.requireNonNull(failures, "failures"));
            if (ids.contains(null) || errors.contains(null)) {
                throw new IllegalArgumentException("report entries cannot be null");
            }
            this.completed = Collections.unmodifiableList(ids);
            this.failures = Collections.unmodifiableList(errors);
            this.forced = forced;
        }
        /** @return terminal state */ public State state() { return state; }
        /** @return completed component IDs in execution order */ public List<DefinitionId> completed() { return completed; }
        /** @return isolated structured failures */ public List<FailureReport> failures() { return failures; }
        /** @return whether force-stop ran */ public boolean forced() { return forced; }
    }
}
