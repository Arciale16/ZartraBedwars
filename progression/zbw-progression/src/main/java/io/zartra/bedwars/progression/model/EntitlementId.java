package io.zartra.bedwars.progression.model;

import io.zartra.bedwars.api.identity.NamespacedIdentifier;

/** Stable entitlement or unlock identity. */
public final class EntitlementId extends NamespacedIdentifier {
    private EntitlementId(final String namespace, final String path) { super(namespace, path); }
    /** @return typed entitlement identity */ public static EntitlementId of(final String namespace, final String path) { return new EntitlementId(namespace, path); }
    /** @return parsed entitlement identity */ public static EntitlementId parse(final String value) {
        final String[] parts = split(value);
        return of(parts[0], parts[1]);
    }
}
