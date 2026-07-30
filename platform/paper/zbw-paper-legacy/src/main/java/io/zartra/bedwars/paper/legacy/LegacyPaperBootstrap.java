package io.zartra.bedwars.paper.legacy;

import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.command.api.CommandFramework;
import io.zartra.bedwars.compat.api.CompatibilityAdapter;
import io.zartra.bedwars.compat.api.CompatibilityAdapterSelector;
import io.zartra.bedwars.compat.api.CompatibilityPresentationBootstrap;
import io.zartra.bedwars.compat.api.CompatibilityRuntimeLifecycle;
import io.zartra.bedwars.compat.v1_10.Paper110CompatibilityAdapter;
import io.zartra.bedwars.compat.v1_11.Paper111CompatibilityAdapter;
import io.zartra.bedwars.compat.v1_8.Paper18CompatibilityAdapter;
import io.zartra.bedwars.compat.v1_9.Paper19CompatibilityAdapter;
import io.zartra.bedwars.ui.api.UiFramework;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

/** Java 8 Paper bootstrap selecting exactly one locked 1.8.8-1.11.2 adapter. */
public final class LegacyPaperBootstrap {
    private final CompatibilityPresentationBootstrap<CommandFramework, UiFramework> delegate;

    /** Creates the bootstrap from operator-produced private fixture digests. */
    public LegacyPaperBootstrap(
            final Map<String, String> fixtureDigests,
            final TimeSource timeSource,
            final CommandFramework commands,
            final UiFramework ui,
            final CompatibilityPresentationBootstrap.Presentation<CommandFramework, UiFramework>
                    presentation) {
        Objects.requireNonNull(fixtureDigests, "fixtureDigests");
        final CompatibilityAdapter[] adapters = {
            Paper18CompatibilityAdapter.create(required(fixtureDigests, "1.8.8"), timeSource),
            Paper19CompatibilityAdapter.create(required(fixtureDigests, "1.9.4"), timeSource),
            Paper110CompatibilityAdapter.create(required(fixtureDigests, "1.10.2"), timeSource),
            Paper111CompatibilityAdapter.create(required(fixtureDigests, "1.11.2"), timeSource)
        };
        delegate = new CompatibilityPresentationBootstrap<CommandFramework, UiFramework>(
                new CompatibilityRuntimeLifecycle(
                        new CompatibilityAdapterSelector(Arrays.asList(adapters))),
                commands, ui, presentation);
    }

    /** Starts only after the exact platform, version, build and digest match. */
    public CompletionStage<Void> start(final CompatibilityAdapter.RuntimeClaim runtime) {
        return delegate.start(runtime);
    }

    /** Releases presentation and adapter resources in reverse order. */
    public CompletionStage<Void> stop() { return delegate.stop(); }

    /** @return current bootstrap state */
    public CompatibilityRuntimeLifecycle.State state() { return delegate.state(); }

    private static String required(final Map<String, String> values, final String version) {
        return Objects.requireNonNull(values.get(version), "missing fixture digest " + version);
    }
}
