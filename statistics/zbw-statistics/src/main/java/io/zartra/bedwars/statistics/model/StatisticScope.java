package io.zartra.bedwars.statistics.model;

import io.zartra.bedwars.api.identity.NamespacedIdentifier;

/** Immutable dimension identity for a mode, arena, group, season or extension. */
public final class StatisticScope extends NamespacedIdentifier {
    private StatisticScope(final String namespace, final String path) { super(namespace, path);
    }
    /** Creates a validated scope identity. */ public static StatisticScope of(final String namespace, final String path) { return new StatisticScope(namespace, path);
    }
    /** Parses a canonical scope identity. */ public static StatisticScope parse(final String value) { final String[] parts = split(value);
    return of(parts[0], parts[1]);
    }
}
