package io.zartra.bedwars.api.identity;

/** Immutable namespaced identity for a versioned content definition. */
public final class DefinitionId extends NamespacedIdentifier {
    private DefinitionId(final String namespace, final String path) { super(namespace, path); }
    /** @return typed definition ID @throws IdentifierFormatException if either component is invalid */
    public static DefinitionId of(final String namespace, final String path) { return new DefinitionId(namespace, path); }
    /** @return parsed definition ID @throws IdentifierFormatException if malformed */
    public static DefinitionId parse(final String value) {
        final String[] parts = split(value);
        return of(parts[0], parts[1]);
    }
}
