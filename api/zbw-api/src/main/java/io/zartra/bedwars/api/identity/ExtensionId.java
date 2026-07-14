package io.zartra.bedwars.api.identity;

/** Immutable namespaced extension identity. */
public final class ExtensionId extends NamespacedIdentifier {
    private ExtensionId(final String namespace, final String path) { super(namespace, path); }
    /** @return typed extension ID @throws IdentifierFormatException if either component is invalid */
    public static ExtensionId of(final String namespace, final String path) { return new ExtensionId(namespace, path); }
    /** @return parsed extension ID @throws IdentifierFormatException if malformed */
    public static ExtensionId parse(final String value) {
        final String[] parts = split(value);
        return of(parts[0], parts[1]);
    }
}
