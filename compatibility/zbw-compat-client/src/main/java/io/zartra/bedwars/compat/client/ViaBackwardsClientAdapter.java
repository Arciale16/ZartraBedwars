package io.zartra.bedwars.compat.client;

import java.util.EnumSet;
import java.util.Set;

/** Optional ViaBackwards 5.4.2 client protocol adapter. */
public final class ViaBackwardsClientAdapter extends AbstractClientPathAdapter {
    private static final Set<ClientFeature> FALLBACKS = EnumSet.of(
            ClientFeature.TEXT, ClientFeature.SOUND, ClientFeature.PARTICLE,
            ClientFeature.ENTITY_DISPLAY, ClientFeature.INPUT);

    /** Creates the exact-version ViaVersion/ViaBackwards path adapter. */
    public ViaBackwardsClientAdapter() {
        super(ClientPath.VIABACKWARDS,
                ClientProvider.VIAVERSION, ClientProvider.VIABACKWARDS);
    }

    @Override
    ClientFeatureOutcome usable(
            final ClientFeature feature, final ClientSession session) {
        final ClientFeatureOutcome.State state = FALLBACKS.contains(feature)
                ? ClientFeatureOutcome.State.FALLBACK
                : ClientFeatureOutcome.State.TRANSLATED;
        return outcome(feature, state, "viabackwards-5_4_2",
                true, false);
    }
}
