package io.zartra.bedwars.progression.cosmetic;

import io.zartra.bedwars.api.identity.NamespacedIdentifier;

/** Stable identity of a cosmetic category. */
public final class CosmeticCategoryId extends NamespacedIdentifier {
    private CosmeticCategoryId(final String namespace, final String path) { super(namespace, path); }
    /** @return typed category identity */
    public static CosmeticCategoryId of(final String namespace, final String path) {
        return new CosmeticCategoryId(namespace, path);
    }
}
