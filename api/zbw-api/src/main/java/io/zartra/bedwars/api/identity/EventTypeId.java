package io.zartra.bedwars.api.identity;

/** Immutable namespaced identity for a versioned event type. */
public final class EventTypeId extends NamespacedIdentifier {
    private EventTypeId(final String namespace, final String path) { super(namespace, path); }
    /** @return typed event-type ID @throws IdentifierFormatException if either component is invalid */
    public static EventTypeId of(final String namespace, final String path) { return new EventTypeId(namespace, path); }
    /** @return parsed event-type ID @throws IdentifierFormatException if malformed */
    public static EventTypeId parse(final String value) {
        final String[] parts = split(value);
        return of(parts[0], parts[1]);
    }
}
