package io.zartra.bedwars.progression.objective;

import io.zartra.bedwars.api.identity.NamespacedIdentifier;

/** Stable allow-listed event type consumed by an objective. */
public final class ObjectiveEventType extends NamespacedIdentifier {
    private ObjectiveEventType(final String namespace, final String path) { super(namespace, path); }
    /** @return validated event type */ public static ObjectiveEventType of(final String namespace, final String path) {
        return new ObjectiveEventType(namespace, path);
    }
    /** @return parsed event type */ public static ObjectiveEventType parse(final String value) {
        final String[] parts = split(value);
        return of(parts[0], parts[1]);
    }
}
