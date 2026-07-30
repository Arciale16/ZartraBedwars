package io.zartra.bedwars.compat.v1_8;

import io.zartra.bedwars.api.identity.ProviderId;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.compat.api.CompatibilityAdapter;
import io.zartra.bedwars.compat.api.CompatibilityMapping;
import io.zartra.bedwars.compat.api.CompatibilityMappingKeys;
import io.zartra.bedwars.compat.api.CompatibilityMappings;
import io.zartra.bedwars.compat.api.SemanticKey;
import io.zartra.bedwars.compat.api.VersionedCompatibilityAdapter;
import java.util.List;

/** Minecraft 1.8.8 symbolic compatibility profile bound to an operator-built locked fixture. */
public final class Paper18CompatibilityAdapter {
    private Paper18CompatibilityAdapter() { throw new AssertionError("No instances"); }

    /** Creates the adapter only after the private BuildTools fixture has a verified digest. */
    public static CompatibilityAdapter create(final String fixtureSha256,
                                              final TimeSource timeSource) {
        final List<CompatibilityMapping> mappings = CompatibilityMappings.complete(
                "paper18", "WOOL:0", "INK_SACK:10", "NBT_COMPOUND", "LEVEL_UP",
                "VILLAGER_HAPPY", "LEGACY_SECTION_TEXT", "ARMOR_STAND",
                "CHAT_POSITION_2", "CHEST", "BUKKIT_SYNC");
        return new VersionedCompatibilityAdapter(
                new CompatibilityAdapter.RuntimeClaim(
                        "BuildTools", "1.8.8", "rev-1.8.8", fixtureSha256),
                ProviderId.of("zartra", "paper18_compat"), mappings,
                CompatibilityMappingKeys.of(mappings, SemanticKey.Kind.MATERIAL,
                        SemanticKey.Kind.ITEM, SemanticKey.Kind.METADATA,
                        SemanticKey.Kind.TEXT, SemanticKey.Kind.PACKET,
                        SemanticKey.Kind.USER_INTERFACE),
                CompatibilityMappingKeys.of(mappings, SemanticKey.Kind.PARTICLE),
                timeSource);
    }
}
