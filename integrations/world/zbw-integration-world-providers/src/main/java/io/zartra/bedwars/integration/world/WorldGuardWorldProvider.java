package io.zartra.bedwars.integration.world;

import io.zartra.bedwars.api.identity.CapabilityId;
import io.zartra.bedwars.api.identity.ProviderId;
import io.zartra.bedwars.api.provider.OptionalProviderLifecycle;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.api.version.SemanticVersion;
import io.zartra.bedwars.world.api.WorldProvider;
import java.util.Collections;

/** Optional WorldGuard 7.0.17 region-protection adapter. */
public final class WorldGuardWorldProvider extends AbstractOptionalWorldProvider {
    /** Creates a vendor-isolated adapter with the native provider as fallback. */
    public WorldGuardWorldProvider(
            final WorldProviderGateway gateway,
            final OptionalProviderLifecycle.Probe probe,
            final WorldProvider fallback,
            final TimeSource timeSource) {
        super(ProviderId.of("zartra", "worldguard"),
                SemanticVersion.parse("7.0.17"),
                Collections.singletonList(
                        CapabilityId.of("zartra", "region_protection")),
                gateway, probe, fallback, timeSource, "provider.worldguard");
    }
}
