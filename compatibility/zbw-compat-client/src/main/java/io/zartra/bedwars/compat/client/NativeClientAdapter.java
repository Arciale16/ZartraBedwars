package io.zartra.bedwars.compat.client;

/** Native Java client parity adapter; no optional provider is required. */
public final class NativeClientAdapter extends AbstractClientPathAdapter {
    /** Creates the native path adapter. */
    public NativeClientAdapter() {
        super(ClientPath.NATIVE);
    }

    @Override
    ClientFeatureOutcome usable(
            final ClientFeature feature, final ClientSession session) {
        return outcome(feature, ClientFeatureOutcome.State.NATIVE,
                "native", true, false);
    }
}
