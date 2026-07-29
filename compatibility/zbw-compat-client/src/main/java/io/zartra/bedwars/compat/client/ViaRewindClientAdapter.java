package io.zartra.bedwars.compat.client;

import java.util.EnumSet;
import java.util.Set;

/** Optional ViaRewind 4.0.6 legacy client protocol adapter. */
public final class ViaRewindClientAdapter extends AbstractClientPathAdapter {
    private static final Set<ClientFeature> DECORATIVE = EnumSet.of(
            ClientFeature.SOUND, ClientFeature.PARTICLE,
            ClientFeature.ENTITY_DISPLAY);

    /** Creates the exact-version Via stack for legacy Java clients. */
    public ViaRewindClientAdapter() {
        super(ClientPath.VIAREWIND,
                ClientProvider.VIAVERSION, ClientProvider.VIABACKWARDS,
                ClientProvider.VIAREWIND);
    }

    @Override
    ClientFeatureOutcome usable(
            final ClientFeature feature, final ClientSession session) {
        if (DECORATIVE.contains(feature)) {
            return outcome(feature, ClientFeatureOutcome.State.DEGRADED,
                    "viarewind-visible-cue", true, true);
        }
        return outcome(feature, ClientFeatureOutcome.State.FALLBACK,
                "viarewind-4_0_6", true, false);
    }
}
