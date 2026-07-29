package io.zartra.bedwars.compat.v1_12_16;

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

/** Exact Paper adapters for the Java 11 compatibility fixtures from 1.12.2 through 1.15.2. */
public final class Paper112To1164CompatibilityAdapters {
    private Paper112To1164CompatibilityAdapters() { throw new AssertionError("No instances"); }

    /** @return immutable exact-runtime adapter inventory */
    public static List<CompatibilityAdapter> all(final TimeSource timeSource) {
        return Collections.unmodifiableList(Arrays.asList(
                create("1.12.2", "1620",
                        "3a2041807f492dcdc34ebb324a287414946e3e05ec3df6fd03f5b5f7d9afc210",
                        true, timeSource),
                create("1.13.2", "657",
                        "11e828d0565ab76a0a0e180c056364a95de44958cfd6a6af3f9b1dc70b03e9cd",
                        false, timeSource),
                create("1.14.4", "245",
                        "bd8ec5cdb22370d37816a6de26798df3d2b0d6f9c7c96c88ca45a1303fea50e8",
                        false, timeSource),
                create("1.15.2", "393",
                        "bd2dd6f2cc489cf9e2bb800cb4fb6d63e9d293945d3ac10b09dd9c6098fa9f34",
                        false, timeSource)));
    }

    private static CompatibilityAdapter create(final String version, final String build,
                                               final String sha256, final boolean legacyMaterial,
                                               final TimeSource timeSource) {
        final String prefix = "paper" + version.replace(".", "");
        final List<CompatibilityMapping> mappings = CompatibilityMappings.complete(
                prefix, legacyMaterial ? "WOOL:0" : "WHITE_WOOL",
                legacyMaterial ? "INK_SACK:10" : "LIME_DYE", "NBT_COMPOUND",
                "ENTITY_PLAYER_LEVELUP", "VILLAGER_HAPPY",
                "LEGACY_SECTION_TEXT", "ARMOR_STAND", "CHAT_POSITION_2",
                "CHEST", "BUKKIT_SYNC");
        return new VersionedCompatibilityAdapter(
                new CompatibilityAdapter.RuntimeClaim("Paper", version, build, sha256),
                ProviderId.of("zartra", prefix + "_compat"), mappings,
                CompatibilityMappingKeys.of(mappings, SemanticKey.Kind.METADATA,
                        SemanticKey.Kind.TEXT, SemanticKey.Kind.PACKET),
                CompatibilityMappingKeys.of(mappings, SemanticKey.Kind.PARTICLE),
                timeSource);
    }
}
