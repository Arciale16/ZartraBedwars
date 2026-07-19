package io.zartra.bedwars.progression.model;

import io.zartra.bedwars.api.identity.NamespacedIdentifier;

/** Stable reward definition or delivery identity. */
public final class RewardId extends NamespacedIdentifier {
    private RewardId(final String namespace, final String path) { super(namespace, path); }
    /** @return typed reward identity */ public static RewardId of(final String namespace, final String path) { return new RewardId(namespace, path); }
    /** @return parsed reward identity */ public static RewardId parse(final String value) {
        final String[] parts = split(value);
        return of(parts[0], parts[1]);
    }
}
