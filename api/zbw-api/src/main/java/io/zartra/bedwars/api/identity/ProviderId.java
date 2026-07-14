package io.zartra.bedwars.api.identity;

/** Immutable namespaced identity for a provider implementation. */
public final class ProviderId extends NamespacedIdentifier {
    private ProviderId(final String namespace, final String path) { super(namespace, path); }
    /** @return typed provider ID @throws IdentifierFormatException if either component is invalid */
    public static ProviderId of(final String namespace, final String path) { return new ProviderId(namespace, path); }
    /** @return parsed provider ID @throws IdentifierFormatException if malformed */
    public static ProviderId parse(final String value) {
        final String[] parts = split(value);
        return of(parts[0], parts[1]);
    }
}
