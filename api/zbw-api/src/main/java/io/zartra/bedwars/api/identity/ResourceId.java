package io.zartra.bedwars.api.identity;

/** Immutable namespaced identity for a native or extension-defined resource. */
public final class ResourceId extends NamespacedIdentifier {
    private ResourceId(final String namespace, final String path) { super(namespace, path); }
    /** @return typed resource ID @throws IdentifierFormatException if either component is invalid */
    public static ResourceId of(final String namespace, final String path) { return new ResourceId(namespace, path); }
    /** @return parsed resource ID @throws IdentifierFormatException if malformed */
    public static ResourceId parse(final String value) {
        final String[] parts = split(value);
        return of(parts[0], parts[1]);
    }
}
