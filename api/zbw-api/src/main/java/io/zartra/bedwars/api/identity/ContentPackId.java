package io.zartra.bedwars.api.identity;

/** Immutable namespaced identity for a content pack. */
public final class ContentPackId extends NamespacedIdentifier {
    private ContentPackId(final String namespace, final String path) { super(namespace, path); }
    /** @return typed content-pack ID @throws IdentifierFormatException if either component is invalid */
    public static ContentPackId of(final String namespace, final String path) { return new ContentPackId(namespace, path); }
    /** @return parsed content-pack ID @throws IdentifierFormatException if malformed */
    public static ContentPackId parse(final String value) {
        final String[] parts = split(value);
        return of(parts[0], parts[1]);
    }
}
