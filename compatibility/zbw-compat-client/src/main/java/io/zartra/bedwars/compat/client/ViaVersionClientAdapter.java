package io.zartra.bedwars.compat.client;

/** Optional ViaVersion 5.4.2 client protocol adapter. */
public final class ViaVersionClientAdapter extends AbstractClientPathAdapter {
    /** Creates the exact-version ViaVersion path adapter. */
    public ViaVersionClientAdapter() {
        super(ClientPath.VIAVERSION, ClientProvider.VIAVERSION);
    }

    @Override
    ClientFeatureOutcome usable(
            final ClientFeature feature, final ClientSession session) {
        return outcome(feature, ClientFeatureOutcome.State.TRANSLATED,
                "viaversion-5_4_2", true, false);
    }
}
