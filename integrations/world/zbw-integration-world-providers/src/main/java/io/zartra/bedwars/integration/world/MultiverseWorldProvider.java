package io.zartra.bedwars.integration.world;

import io.zartra.bedwars.api.identity.CapabilityId;
import io.zartra.bedwars.api.identity.ProviderId;
import io.zartra.bedwars.api.provider.OptionalProviderLifecycle;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.api.version.SemanticVersion;
import io.zartra.bedwars.world.api.WorldProvider;
import java.util.Arrays;

/** Optional Multiverse-Core 5.3.3 managed-world lifecycle adapter. */
public final class MultiverseWorldProvider extends AbstractOptionalWorldProvider {
    /** Creates a vendor-isolated adapter with the native provider as fallback. */
    public MultiverseWorldProvider(
            final WorldProviderGateway gateway,
            final OptionalProviderLifecycle.Probe probe,
            final WorldProvider fallback,
            final TimeSource timeSource) {
        super(ProviderId.of("zartra", "multiverse-core"),
                SemanticVersion.parse("5.3.3"),
                Arrays.asList(CapabilityId.of("zartra", "world_loading"),
                        CapabilityId.of("zartra", "world_registry")),
                gateway, probe, fallback, timeSource, "provider.multiverse");
    }
}
