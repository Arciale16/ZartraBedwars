package io.zartra.bedwars.compat.api;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Nonblocking composition boundary that activates neutral command/UI presentation only after an
 * exact compatibility adapter is running.
 *
 * @param <C> command framework type
 * @param <U> UI framework type
 */
public final class CompatibilityPresentationBootstrap<C, U> {
    private final CompatibilityRuntimeLifecycle lifecycle;
    private final C commandFramework;
    private final U uiFramework;
    private final Presentation<C, U> presentation;
    private boolean presentationActive;

    /** Creates a bootstrap with no platform or business implementation dependency. */
    public CompatibilityPresentationBootstrap(
            final CompatibilityRuntimeLifecycle lifecycle,
            final C commandFramework,
            final U uiFramework,
            final Presentation<C, U> presentation) {
        this.lifecycle = Objects.requireNonNull(lifecycle, "lifecycle");
        this.commandFramework = Objects.requireNonNull(commandFramework, "commandFramework");
        this.uiFramework = Objects.requireNonNull(uiFramework, "uiFramework");
        this.presentation = Objects.requireNonNull(presentation, "presentation");
    }

    /** Starts compatibility first, then presentation; failures trigger compatibility cleanup. */
    public CompletionStage<Void> start(final CompatibilityAdapter.RuntimeClaim runtime) {
        final CompletableFuture<Void> outcome = new CompletableFuture<Void>();
        lifecycle.start(runtime).whenComplete((adapter, selectionFailure) -> {
            if (selectionFailure != null) {
                outcome.completeExceptionally(selectionFailure);
                return;
            }
            final CompletionStage<Void> activation;
            try {
                activation = Objects.requireNonNull(
                        presentation.activate(adapter, commandFramework, uiFramework),
                        "presentation activation");
            } catch (RuntimeException failure) {
                lifecycle.stop().whenComplete((ignored, cleanupFailure) ->
                        outcome.completeExceptionally(failure));
                return;
            }
            activation.whenComplete((ignored, activationFailure) -> {
                if (activationFailure == null) {
                    synchronized (CompatibilityPresentationBootstrap.this) {
                        presentationActive = true;
                    }
                    outcome.complete(null);
                } else {
                    lifecycle.stop().whenComplete((unused, cleanupFailure) ->
                            outcome.completeExceptionally(activationFailure));
                }
            });
        });
        return outcome;
    }

    /** Deactivates presentation before stopping the adapter. */
    public CompletionStage<Void> stop() {
        final CompletableFuture<Void> outcome = new CompletableFuture<Void>();
        final CompletionStage<Void> deactivation;
        synchronized (this) {
            deactivation = presentationActive
                    ? presentation.deactivate() : CompletableFuture.completedFuture(null);
            presentationActive = false;
        }
        deactivation.whenComplete((ignored, presentationFailure) ->
                lifecycle.stop().whenComplete((unused, lifecycleFailure) -> {
                    if (presentationFailure != null) {
                        outcome.completeExceptionally(presentationFailure);
                    } else if (lifecycleFailure != null) {
                        outcome.completeExceptionally(lifecycleFailure);
                    } else {
                        outcome.complete(null);
                    }
                }));
        return outcome;
    }

    /** @return compatibility lifecycle state */
    public CompatibilityRuntimeLifecycle.State state() { return lifecycle.state(); }

    /** Platform presentation port implemented by the concrete Paper bootstrap. */
    public interface Presentation<C, U> {
        /** Activates command and UI presentation without blocking the owner thread. */
        CompletionStage<Void> activate(CompatibilityAdapter adapter, C commands, U ui);
        /** Releases listeners, scheduled work and presentation resources. */
        CompletionStage<Void> deactivate();
    }
}
