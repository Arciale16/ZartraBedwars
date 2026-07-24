package io.zartra.bedwars.progression.cosmetic;

import io.zartra.bedwars.api.identity.NamespacedIdentifier;

/** Stable identity of a configurable cosmetic rarity. */
public final class CosmeticRarityId extends NamespacedIdentifier {
    private CosmeticRarityId(final String namespace, final String path) { super(namespace, path); }
    /** @return typed rarity identity */
    public static CosmeticRarityId of(final String namespace, final String path) {
        return new CosmeticRarityId(namespace, path);
    }
}
