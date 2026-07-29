package io.zartra.bedwars.compat.v1_16_5;

import io.zartra.bedwars.api.identity.ProviderId;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.compat.api.CompatibilityAdapter;
import io.zartra.bedwars.compat.api.CompatibilityMapping;
import io.zartra.bedwars.compat.api.CompatibilityMappingKeys;
import io.zartra.bedwars.compat.api.CompatibilityMappings;
import io.zartra.bedwars.compat.api.SemanticKey;
import io.zartra.bedwars.compat.api.VersionedCompatibilityAdapter;
import java.util.List;

/** Exact Paper 1.16.5 compatibility adapter. */
public final class Paper1165CompatibilityAdapter {
    /** Locked Paper fixture digest. */
    public static final String SERVER_SHA256 =
            "e67da4851d08cde378ab2b89be58849238c303351ed2482181a99c2c2b489276";
    private Paper1165CompatibilityAdapter() { throw new AssertionError("No instances"); }

    /** @return exact-runtime adapter with declared legacy fallbacks */
    public static CompatibilityAdapter create(final TimeSource timeSource) {
        final List<CompatibilityMapping> mappings = CompatibilityMappings.complete(
                "paper1165", "WHITE_WOOL", "LIME_DYE", "NBT_COMPOUND",
                "ENTITY_PLAYER_LEVELUP", "VILLAGER_HAPPY", "BUNGEE_COMPONENT",
                "ARMOR_STAND", "CHAT_POSITION_2", "CHEST", "BUKKIT_SYNC");
        return new VersionedCompatibilityAdapter(
                new CompatibilityAdapter.RuntimeClaim(
                        "Paper", "1.16.5", "794", SERVER_SHA256),
                ProviderId.of("zartra", "paper1165_compat"), mappings,
                CompatibilityMappingKeys.of(mappings, SemanticKey.Kind.METADATA,
                        SemanticKey.Kind.TEXT, SemanticKey.Kind.PACKET),
                CompatibilityMappingKeys.of(mappings, SemanticKey.Kind.PARTICLE),
                timeSource);
    }
}
