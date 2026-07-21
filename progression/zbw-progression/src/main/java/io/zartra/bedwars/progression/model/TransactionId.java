package io.zartra.bedwars.progression.model;

import io.zartra.bedwars.api.identity.NamespacedIdentifier;

/** Stable economic transaction identity. */
public final class TransactionId extends NamespacedIdentifier {
    private TransactionId(final String namespace, final String path) { super(namespace, path); }
    /** @return typed transaction identity */ public static TransactionId of(final String namespace, final String path) { return new TransactionId(namespace, path); }
    /** @return parsed transaction identity */ public static TransactionId parse(final String value) {
        final String[] parts = split(value);
        return of(parts[0], parts[1]);
    }
}
