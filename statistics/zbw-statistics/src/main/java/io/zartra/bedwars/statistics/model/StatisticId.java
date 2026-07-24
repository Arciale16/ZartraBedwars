package io.zartra.bedwars.statistics.model;

import io.zartra.bedwars.api.identity.NamespacedIdentifier;

/** Immutable namespaced identity for a statistic definition. */
public final class StatisticId extends NamespacedIdentifier {
    private StatisticId(final String namespace, final String path) {
        super(namespace, path);
    }

    /** Creates a validated statistic identity. */
    public static StatisticId of(final String namespace, final String path) {
        return new StatisticId(namespace, path);
    }

    /** Parses a canonical statistic identity. */
    public static StatisticId parse(final String value) {
        final String[] parts = split(value);
        return of(parts[0], parts[1]);
    }
}
