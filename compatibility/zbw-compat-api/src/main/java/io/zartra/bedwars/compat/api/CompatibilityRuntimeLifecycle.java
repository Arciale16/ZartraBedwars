package io.zartra.bedwars.compat.api;

import io.zartra.bedwars.api.provider.Provider;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/** Nonblocking fail-closed lifecycle for one selected compatibility adapter. */
public final class CompatibilityRuntimeLifecycle {
    private final CompatibilityAdapterSelector selector;
    private CompatibilityAdapter active;
    private State state = State.NEW;

    /** Creates a lifecycle around an exact-runtime selector. */
    public CompatibilityRuntimeLifecycle(final CompatibilityAdapterSelector selector) {
        this.selector = Objects.requireNonNull(selector, "selector");
    }

    /**
     * Selects and starts exactly one adapter before feature presentation may be activated.
     *
     * @return eventual selected adapter
     */
    public synchronized CompletionStage<CompatibilityAdapter> start(
            final CompatibilityAdapter.RuntimeClaim runtime) {
        if (state != State.NEW && state != State.STOPPED) {
            throw new IllegalStateException("compatibility runtime already active");
        }
        final CompatibilityAdapter selected = selector.select(runtime);
        state = State.STARTING;
        final CompletableFuture<CompatibilityAdapter> outcome =
                new CompletableFuture<CompatibilityAdapter>();
        selected.start().whenComplete((result, failure) -> {
            synchronized (CompatibilityRuntimeLifecycle.this) {
                if (failure != null || result == null || result.isFailure()
                        || result.value().orElse(Provider.LifecycleState.STOPPED)
                        != Provider.LifecycleState.RUNNING) {
                    state = State.FAILED;
                    outcome.completeExceptionally(new IllegalStateException(
                            "compatibility adapter failed before feature activation", failure));
                    return;
                }
                active = selected;
                state = State.RUNNING;
                outcome.complete(selected);
            }
        });
        return outcome;
    }

    /** Stops the selected adapter and releases its reference. */
    public synchronized CompletionStage<Void> stop() {
        if (active == null) {
            state = State.STOPPED;
            return CompletableFuture.completedFuture(null);
        }
        state = State.STOPPING;
        final CompatibilityAdapter stopping = active;
        final CompletableFuture<Void> outcome = new CompletableFuture<Void>();
        stopping.stop().whenComplete((result, failure) -> {
            synchronized (CompatibilityRuntimeLifecycle.this) {
                active = null;
                if (failure != null || result == null || result.isFailure()) {
                    state = State.FAILED;
                    outcome.completeExceptionally(new IllegalStateException(
                            "compatibility adapter cleanup failed", failure));
                } else {
                    state = State.STOPPED;
                    outcome.complete(null);
                }
            }
        });
        return outcome;
    }

    /** @return current lifecycle state */
    public synchronized State state() { return state; }

    /** @return selected adapter, or {@code null} before activation and after cleanup */
    public synchronized CompatibilityAdapter active() { return active; }

    /** Runtime lifecycle states. */
    public enum State { NEW, STARTING, RUNNING, STOPPING, STOPPED, FAILED }
}
