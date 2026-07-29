package io.zartra.bedwars.integration.world;

import io.zartra.bedwars.api.identity.CapabilityId;
import io.zartra.bedwars.api.identity.ProviderId;
import io.zartra.bedwars.api.provider.OptionalProviderLifecycle;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.api.version.SemanticVersion;
import io.zartra.bedwars.world.api.WorldProvider;
import java.util.Arrays;

/** Optional FastAsyncWorldEdit 2.15.1 bounded fast-edit/reset adapter. */
public final class FaweWorldProvider extends AbstractOptionalWorldProvider {
    /** Creates a vendor-isolated adapter with the native provider as fallback. */
    public FaweWorldProvider(
            final WorldProviderGateway gateway,
            final OptionalProviderLifecycle.Probe probe,
            final WorldProvider fallback,
            final TimeSource timeSource) {
        super(ProviderId.of("zartra", "fawe"),
                SemanticVersion.parse("2.15.1"),
                Arrays.asList(CapabilityId.of("zartra", "world_fast_edit"),
                        CapabilityId.of("zartra", "world_fast_reset")),
                gateway, probe, fallback, timeSource, "provider.fawe");
    }
}
