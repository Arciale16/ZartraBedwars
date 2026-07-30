package io.zartra.bedwars.compat.v1_17_19;

import io.zartra.bedwars.api.identity.ProviderId;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.compat.api.CompatibilityAdapter;
import io.zartra.bedwars.compat.api.CompatibilityMapping;
import io.zartra.bedwars.compat.api.CompatibilityMappingKeys;
import io.zartra.bedwars.compat.api.CompatibilityMappings;
import io.zartra.bedwars.compat.api.SemanticKey;
import io.zartra.bedwars.compat.api.VersionedCompatibilityAdapter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Exact Paper adapters for the Java 17 fixtures from 1.17.1 through 1.19.4. */
public final class Paper117To119CompatibilityAdapters {
    private Paper117To119CompatibilityAdapters() { throw new AssertionError("No instances"); }

    /** @return immutable exact-runtime adapter inventory */
    public static List<CompatibilityAdapter> all(final TimeSource timeSource) {
        return Collections.unmodifiableList(Arrays.asList(
                create("1.17.1", "411",
                        "6cc1ee2f94253ce10b5374ed85fffc735a97d8f1b64db293683dfa24dd3cc05f",
                        timeSource),
                create("1.18.2", "388",
                        "0578f18f4d632b494b468ec56b3b414b5b56fea087ee7d39cf6dcdf4c9d01f05",
                        timeSource),
                create("1.19.4", "550",
                        "e587d78cba3e99ef8c4bc24cf20cc3bdbbe89e33b0b572070446af4eb6be5ccf",
                        timeSource)));
    }

    private static CompatibilityAdapter create(final String version, final String build,
                                               final String sha256,
                                               final TimeSource timeSource) {
        final String prefix = "paper" + version.replace(".", "");
        final List<CompatibilityMapping> mappings = CompatibilityMappings.complete(
                prefix, "WHITE_WOOL", "LIME_DYE", "PERSISTENT_DATA_CONTAINER",
                "ENTITY_EXPERIENCE_ORB_PICKUP", "VILLAGER_HAPPY", "ADVENTURE_COMPONENT",
                "ARMOR_STAND", "PAPER_PACKET_BRIDGE", "CHEST", "BUKKIT_SYNC");
        return new VersionedCompatibilityAdapter(
                new CompatibilityAdapter.RuntimeClaim("Paper", version, build, sha256),
                ProviderId.of("zartra", prefix + "_compat"), mappings,
                CompatibilityMappingKeys.of(mappings, SemanticKey.Kind.PACKET),
                Collections.<SemanticKey>emptySet(), timeSource);
    }
}
