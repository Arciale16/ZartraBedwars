package io.zartra.bedwars.integration.world;

import io.zartra.bedwars.api.identity.CapabilityId;
import io.zartra.bedwars.api.identity.ProviderId;
import io.zartra.bedwars.api.provider.OptionalProviderLifecycle;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.api.version.SemanticVersion;
import io.zartra.bedwars.world.api.WorldProvider;
import java.util.Arrays;

/** Optional WorldEdit 7.3.16 world editing and selection adapter. */
public final class WorldEditWorldProvider extends AbstractOptionalWorldProvider {
    /** Creates a vendor-isolated adapter with the native provider as fallback. */
    public WorldEditWorldProvider(
            final WorldProviderGateway gateway,
            final OptionalProviderLifecycle.Probe probe,
            final WorldProvider fallback,
            final TimeSource timeSource) {
        super(ProviderId.of("zartra", "worldedit"),
                SemanticVersion.parse("7.3.16"),
                Arrays.asList(CapabilityId.of("zartra", "world_edit"),
                        CapabilityId.of("zartra", "world_selection")),
                gateway, probe, fallback, timeSource, "provider.worldedit");
    }
}
