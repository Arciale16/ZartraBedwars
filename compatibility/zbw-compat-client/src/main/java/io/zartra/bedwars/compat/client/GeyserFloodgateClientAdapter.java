package io.zartra.bedwars.compat.client;

import java.util.EnumSet;
import java.util.Set;

/** Optional Geyser 2.7.0 and Floodgate 2.2.4 Bedrock client adapter. */
public final class GeyserFloodgateClientAdapter
        extends AbstractClientPathAdapter {
    private static final Set<ClientFeature> DECORATIVE = EnumSet.of(
            ClientFeature.SOUND, ClientFeature.PARTICLE,
            ClientFeature.ENTITY_DISPLAY);
    private static final Set<ClientFeature> DIRECT = EnumSet.of(
            ClientFeature.TEXT);

    /** Creates the exact-version paired Bedrock path adapter. */
    public GeyserFloodgateClientAdapter() {
        super(ClientPath.GEYSER_FLOODGATE,
                ClientProvider.GEYSER, ClientProvider.FLOODGATE);
    }

    @Override
    ClientFeatureOutcome usable(
            final ClientFeature feature, final ClientSession session) {
        if (DECORATIVE.contains(feature)) {
            return outcome(feature, ClientFeatureOutcome.State.DEGRADED,
                    "bedrock-visible-cue", true, true);
        }
        if (DIRECT.contains(feature)) {
            return outcome(feature, ClientFeatureOutcome.State.TRANSLATED,
                    "geyser-2_7_0", true, false);
        }
        final String reason = feature == ClientFeature.INPUT
                ? "floodgate-input-" + session.inputMode().name()
                        .toLowerCase(java.util.Locale.ROOT)
                : "bedrock-equivalent";
        return outcome(feature, ClientFeatureOutcome.State.FALLBACK,
                reason, true, false);
    }
}
