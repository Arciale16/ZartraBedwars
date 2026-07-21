package io.zartra.bedwars.progression.objective;

import io.zartra.bedwars.api.identity.NamespacedIdentifier;

/** Stable typed identity of an objective definition. */
public final class ObjectiveId extends NamespacedIdentifier {
    private ObjectiveId(final String namespace, final String path) {
        super(namespace, path);
    }

    /** @return a validated objective identity */
    public static ObjectiveId of(final String namespace, final String path) {
        return new ObjectiveId(namespace, path);
    }

    /** @return a parsed objective identity */
    public static ObjectiveId parse(final String value) {
        final String[] parts = split(value);
        return of(parts[0], parts[1]);
    }
}
