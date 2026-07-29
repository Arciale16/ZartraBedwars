package io.zartra.bedwars.integration.world;

import io.zartra.bedwars.api.identity.CapabilityId;
import io.zartra.bedwars.api.identity.ProviderId;
import io.zartra.bedwars.api.provider.OptionalProviderLifecycle;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.api.version.SemanticVersion;
import io.zartra.bedwars.world.api.WorldProvider;
import java.util.Arrays;

/** Optional AdvancedSlimePaper/SlimeWorldManager 5.1.0 lifecycle adapter. */
public final class SlimeWorldManagerWorldProvider
        extends AbstractOptionalWorldProvider {
    /** Creates a vendor-isolated adapter with the native provider as fallback. */
    public SlimeWorldManagerWorldProvider(
            final WorldProviderGateway gateway,
            final OptionalProviderLifecycle.Probe probe,
            final WorldProvider fallback,
            final TimeSource timeSource) {
        super(ProviderId.of("zartra", "slimeworldmanager"),
                SemanticVersion.parse("5.1.0"),
                Arrays.asList(CapabilityId.of("zartra", "world_loading"),
                        CapabilityId.of("zartra", "world_snapshot")),
                gateway, probe, fallback, timeSource,
                "provider.slimeworldmanager");
    }
}
