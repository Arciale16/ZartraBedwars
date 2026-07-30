package io.zartra.bedwars.compat.modern;

import io.zartra.bedwars.api.identity.ProviderId;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.compat.api.CompatibilityAdapter;
import io.zartra.bedwars.compat.api.CompatibilityMapping;
import io.zartra.bedwars.compat.api.CompatibilityMappingKeys;
import io.zartra.bedwars.compat.api.CompatibilityMappings;
import io.zartra.bedwars.compat.api.SemanticKey;
import io.zartra.bedwars.compat.api.VersionedCompatibilityAdapter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Exact modern Paper adapter inventory for every locked 1.20.x and 1.21.x fixture. */
public final class ModernCompatibilityAdapters {
    private static final String[][] FIXTURES = {
        {"1.20.1", "196", "234a9b32098100c6fc116664d64e36ccdb58b5b649af0f80bcccb08b0255eaea"},
        {"1.20.2", "318", "ba340a835ac40b8563aa7eda1cd6479a11a7623409c89a2c35cd9d7490ed17a7"},
        {"1.20.4", "499", "cabed3ae77cf55deba7c7d8722bc9cfd5e991201c211665f9265616d9fe5c77b"},
        {"1.20.6", "151", "4b011f5adb5f6c72007686a223174fce82f31aeb4b34faf4652abc840b47e640"},
        {"1.21.3", "83", "87e973e1d338e869e7fdbc4b8fadc1579d7bb0246a0e0cf6e5700ace6c8bc17e"},
        {"1.21.4", "232", "5ee4f542f628a14c644410b08c94ea42e772ef4d29fe92973636b6813d4eaffc"},
        {"1.21.8", "60", "8de7c52c3b02403503d16fac58003f1efef7dd7a0256786843927fa92ee57f1e"},
        {"1.21.10", "130", "158703f75a26f842ea656b3dc6d75bf3d1ec176b97a2c36384d0b80b3871af53"},
        {"1.21.11", "132", "5ffef465eeeb5f2a3c23a24419d97c51afd7dbb4923ff42df9a3f58bba1ccfba"}
    };

    private ModernCompatibilityAdapters() { throw new AssertionError("No instances"); }

    /** @return immutable exact-runtime inventory, including the M06-certified 1.21.1 adapter */
    public static List<CompatibilityAdapter> all(final TimeSource timeSource) {
        final List<CompatibilityAdapter> adapters = new ArrayList<CompatibilityAdapter>();
        for (String[] fixture : FIXTURES) {
            adapters.add(create(fixture[0], fixture[1], fixture[2], timeSource));
        }
        adapters.add(new Paper121CompatibilityAdapter(timeSource));
        return Collections.unmodifiableList(adapters);
    }

    private static CompatibilityAdapter create(final String version, final String build,
                                               final String sha256,
                                               final TimeSource timeSource) {
        final String prefix = "paper" + version.replace(".", "");
        final List<CompatibilityMapping> mappings = CompatibilityMappings.complete(
                prefix, "WHITE_WOOL", "LIME_DYE", "PERSISTENT_DATA_CONTAINER",
                "ENTITY_EXPERIENCE_ORB_PICKUP", "HAPPY_VILLAGER", "ADVENTURE_COMPONENT",
                "ARMOR_STAND", "PAPER_PACKET_BRIDGE", "CHEST", "BUKKIT_SYNC");
        return new VersionedCompatibilityAdapter(
                new CompatibilityAdapter.RuntimeClaim("Paper", version, build, sha256),
                ProviderId.of("zartra", prefix + "_compat"), mappings,
                CompatibilityMappingKeys.of(mappings, SemanticKey.Kind.PACKET),
                Collections.<SemanticKey>emptySet(), timeSource);
    }
}
