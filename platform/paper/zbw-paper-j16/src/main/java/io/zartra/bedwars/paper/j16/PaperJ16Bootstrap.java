package io.zartra.bedwars.paper.j16;

import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.command.api.CommandFramework;
import io.zartra.bedwars.compat.api.CompatibilityAdapter;
import io.zartra.bedwars.compat.api.CompatibilityAdapterSelector;
import io.zartra.bedwars.compat.api.CompatibilityPresentationBootstrap;
import io.zartra.bedwars.compat.api.CompatibilityRuntimeLifecycle;
import io.zartra.bedwars.compat.v1_16_5.Paper1165CompatibilityAdapter;
import io.zartra.bedwars.ui.api.UiFramework;
import java.util.Collections;
import java.util.concurrent.CompletionStage;

/** Java 16 Paper bootstrap for the exact 1.16.5 fixture. */
public final class PaperJ16Bootstrap {
    private final CompatibilityPresentationBootstrap<CommandFramework, UiFramework> delegate;

    /** Creates a bootstrap over the locked Java 16 adapter. */
    public PaperJ16Bootstrap(
            final TimeSource timeSource,
            final CommandFramework commands,
            final UiFramework ui,
            final CompatibilityPresentationBootstrap.Presentation<CommandFramework, UiFramework>
                    presentation) {
        delegate = new CompatibilityPresentationBootstrap<CommandFramework, UiFramework>(
                new CompatibilityRuntimeLifecycle(new CompatibilityAdapterSelector(
                        Collections.singletonList(
                                Paper1165CompatibilityAdapter.create(timeSource)))),
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
