package io.zartra.bedwars.progression.model;

import io.zartra.bedwars.api.identity.NamespacedIdentifier;

/** Stable identity for a persistent currency, never a match resource. */
public final class CurrencyId extends NamespacedIdentifier {
    private CurrencyId(final String namespace, final String path) { super(namespace, path); }
    /** @return typed currency identity */ public static CurrencyId of(final String namespace, final String path) { return new CurrencyId(namespace, path); }
    /** @return parsed currency identity */ public static CurrencyId parse(final String value) {
        final String[] parts = split(value);
        return of(parts[0], parts[1]);
    }
}
