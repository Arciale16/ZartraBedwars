package io.zartra.bedwars.api.identity;

/** Immutable namespaced identity for a private-game modifier. */
public final class PrivateGameModifierId extends NamespacedIdentifier {
    private PrivateGameModifierId(final String namespace, final String path) { super(namespace, path); }
    /** @return typed modifier ID @throws IdentifierFormatException if either component is invalid */
    public static PrivateGameModifierId of(final String namespace, final String path) { return new PrivateGameModifierId(namespace, path); }
    /** @return parsed modifier ID @throws IdentifierFormatException if malformed */
    public static PrivateGameModifierId parse(final String value) {
        final String[] parts = split(value);
        return of(parts[0], parts[1]);
    }
}
