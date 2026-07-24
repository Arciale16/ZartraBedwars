package io.zartra.bedwars.progression.cosmetic;

import io.zartra.bedwars.api.identity.NamespacedIdentifier;

/** Stable identity of a cosmetic definition. */
public final class CosmeticId extends NamespacedIdentifier {
    private CosmeticId(final String namespace, final String path) { super(namespace, path); }
    /** @return typed cosmetic identity */
    public static CosmeticId of(final String namespace, final String path) {
        return new CosmeticId(namespace, path);
    }
    /** @return parsed cosmetic identity */
    public static CosmeticId parse(final String value) {
        final int separator = value == null ? -1 : value.indexOf(':');
        if (separator < 1 || separator == value.length() - 1) {
            throw new IllegalArgumentException("cosmetic ID must be namespace:path");
        }
        return of(value.substring(0, separator), value.substring(separator + 1));
    }
}
