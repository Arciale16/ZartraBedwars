package io.zartra.bedwars.progression.pass;

import io.zartra.bedwars.api.identity.NamespacedIdentifier;

/** Stable typed identity of a battle-pass season. */
public final class SeasonId extends NamespacedIdentifier {
    private SeasonId(final String namespace, final String path) { super(namespace, path); }
    /** @return validated season identity */ public static SeasonId of(final String namespace, final String path) {
        return new SeasonId(namespace, path);
    }
    /** @return parsed season identity */ public static SeasonId parse(final String value) {
        final String[] parts = split(value);
        return of(parts[0], parts[1]);
    }
}
