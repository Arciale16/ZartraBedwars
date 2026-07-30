package io.zartra.bedwars.paper.j17;

import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.command.api.CommandFramework;
import io.zartra.bedwars.compat.api.CompatibilityAdapter;
import io.zartra.bedwars.compat.api.CompatibilityAdapterSelector;
import io.zartra.bedwars.compat.api.CompatibilityPresentationBootstrap;
import io.zartra.bedwars.compat.api.CompatibilityRuntimeLifecycle;
import io.zartra.bedwars.compat.v1_17_19.Paper117To119CompatibilityAdapters;
import io.zartra.bedwars.ui.api.UiFramework;
import java.util.concurrent.CompletionStage;

/** Java 17 Paper bootstrap for exact 1.17.1-1.19.4 fixtures. */
public final class PaperJ17Bootstrap {
    private final CompatibilityPresentationBootstrap<CommandFramework, UiFramework> delegate;

    /** Creates a bootstrap over the locked Java 17 adapter inventory. */
    public PaperJ17Bootstrap(
            final TimeSource timeSource,
            final CommandFramework commands,
            final UiFramework ui,
            final CompatibilityPresentationBootstrap.Presentation<CommandFramework, UiFramework>
                    presentation) {
        delegate = new CompatibilityPresentationBootstrap<CommandFramework, UiFramework>(
                new CompatibilityRuntimeLifecycle(new CompatibilityAdapterSelector(
                        Paper117To119CompatibilityAdapters.all(timeSource))),
                commands, ui, presentation);
    }

    /** Starts only after an exact locked runtime match. */
    public CompletionStage<Void> start(final CompatibilityAdapter.RuntimeClaim runtime) {
        return delegate.start(runtime);
    }
    /** Releases presentation and adapter resources. */
    public CompletionStage<Void> stop() { return delegate.stop(); }
    /** @return current bootstrap state */
    public CompatibilityRuntimeLifecycle.State state() { return delegate.state(); }
}
