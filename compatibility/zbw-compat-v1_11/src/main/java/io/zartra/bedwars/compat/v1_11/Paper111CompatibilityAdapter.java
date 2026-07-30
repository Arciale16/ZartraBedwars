package io.zartra.bedwars.compat.v1_11;

import io.zartra.bedwars.api.identity.ProviderId;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.compat.api.CompatibilityAdapter;
import io.zartra.bedwars.compat.api.CompatibilityMapping;
import io.zartra.bedwars.compat.api.CompatibilityMappingKeys;
import io.zartra.bedwars.compat.api.CompatibilityMappings;
import io.zartra.bedwars.compat.api.SemanticKey;
import io.zartra.bedwars.compat.api.VersionedCompatibilityAdapter;
import java.util.List;

/** Minecraft 1.11.2 symbolic compatibility profile bound to an operator-built locked fixture. */
public final class Paper111CompatibilityAdapter {
    private Paper111CompatibilityAdapter() { throw new AssertionError("No instances"); }

    /** Creates the adapter only after the private BuildTools fixture has a verified digest. */
    public static CompatibilityAdapter create(final String fixtureSha256,
                                              final TimeSource timeSource) {
        final List<CompatibilityMapping> mappings = CompatibilityMappings.complete(
                "paper111", "WOOL:0", "INK_SACK:10", "NBT_COMPOUND",
                "ENTITY_PLAYER_LEVELUP", "VILLAGER_HAPPY", "LEGACY_SECTION_TEXT",
                "ARMOR_STAND", "CHAT_POSITION_2", "CHEST", "BUKKIT_SYNC");
        return new VersionedCompatibilityAdapter(
                new CompatibilityAdapter.RuntimeClaim(
                        "BuildTools", "1.11.2", "rev-1.11.2", fixtureSha256),
                ProviderId.of("zartra", "paper111_compat"), mappings,
                CompatibilityMappingKeys.of(mappings, SemanticKey.Kind.MATERIAL,
                        SemanticKey.Kind.ITEM, SemanticKey.Kind.METADATA,
                        SemanticKey.Kind.TEXT, SemanticKey.Kind.PACKET),
                CompatibilityMappingKeys.of(mappings, SemanticKey.Kind.PARTICLE),
                timeSource);
    }
}
